package com.app.service;

import com.app.dto.debt.*;
import com.app.model.*;
import com.app.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DebtService {

    private final DebtHeaderRepository debtHeaderRepository;
    private final DebtDetailRepository debtDetailRepository;

    private final ProjectRepository projectRepository;
    private final PersonRepository personRepository;
    private final ItemRepository itemRepository;
    private final UnitRepository unitRepository;

    private final TransactionTrackRepository trackRepository;
    private final JdbcTemplate jdbcTemplate;

    public DebtService(DebtHeaderRepository debtHeaderRepository,
                       DebtDetailRepository debtDetailRepository,
                       ProjectRepository projectRepository,
                       PersonRepository personRepository,
                       ItemRepository itemRepository,
                       UnitRepository unitRepository,
                       TransactionTrackRepository trackRepository,
                       JdbcTemplate jdbcTemplate) {

        this.debtHeaderRepository = debtHeaderRepository;
        this.debtDetailRepository = debtDetailRepository;
        this.projectRepository = projectRepository;
        this.personRepository = personRepository;
        this.itemRepository = itemRepository;
        this.unitRepository = unitRepository;
        this.trackRepository = trackRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------- CRUD ----------------

    @Transactional
    public DebtHeaderResponse create(DebtCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("اطلاعات بدهی ارسال نشده است.");
        if (req.lines() == null || req.lines().isEmpty())
            throw new IllegalArgumentException("حداقل یک ردیف برای بدهی الزامی است.");

        DebtHeader header = new DebtHeader();
        applyHeader(header, req.projectId(), req.personId(), req.dateDue(), req.dateRegistered(), req.dsc());

        try {
            DebtHeader savedHeader = debtHeaderRepository.save(header);
            saveLines(savedHeader, req.lines());
            return toHeaderResponse(savedHeader);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("ثبت بدهی انجام نشد. احتمالاً داده تکراری است یا محدودیت دیتابیس وجود دارد.");
        }
    }

    @Transactional(readOnly = true)
    public DebtHeaderResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");

        DebtHeader h = debtHeaderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + id + ")"));
        return toHeaderResponse(h);
    }

    @Transactional
    public DebtHeaderResponse update(Long id, DebtUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش بدهی ارسال نشده است.");
        if (req.lines() == null || req.lines().isEmpty())
            throw new IllegalArgumentException("حداقل یک ردیف برای بدهی الزامی است.");

        DebtHeader header = debtHeaderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + id + ")"));

        // اگر تخصیص داشته باشد، اجازه ویرایش هدر هست،
        // اما جمع ردیف‌های جدید نباید از مبلغ تخصیص‌داده‌شده کمتر شود.
        BigDecimal alreadyCovered = trackRepository.sumCoveredByDebt(id);
        BigDecimal newTotal = calcTotalFromLines(req.lines());

        if (alreadyCovered != null && newTotal.compareTo(alreadyCovered) < 0) {
            throw new IllegalArgumentException(
                    "مبلغ جدید بدهی نمی‌تواند کمتر از مبلغ تخصیص داده شده باشد. مبلغ تخصیص: " + fmt(alreadyCovered)
            );
        }

        applyHeader(header, req.projectId(), req.personId(), req.dateDue(), req.dateRegistered(), req.dsc());

        try {
            DebtHeader saved = debtHeaderRepository.save(header);

            // جایگزینی کامل ردیف‌ها
            debtDetailRepository.deleteByDebtHeader_Id(id);
            saveLines(saved, req.lines());

            return toHeaderResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("ویرایش بدهی انجام نشد. احتمالاً داده تکراری است یا محدودیت دیتابیس وجود دارد.");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");

        DebtHeader h = debtHeaderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + id + ")"));

        // اگر تخصیص داشته باشد، حذف ممنوع
        BigDecimal covered = trackRepository.sumCoveredByDebt(id);
        if (covered != null && covered.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("امکان حذف بدهی وجود ندارد؛ برای این بدهی تخصیص ثبت شده است.");
        }

        // اگر سند/پیوست داشته باشد، حذف ممنوع (FK)
        if (hasDebtDocuments(id)) {
            throw new IllegalArgumentException("امکان حذف بدهی وجود ندارد؛ برای این بدهی سند/فایل پیوست شده است.");
        }

        // حذف ردیف‌ها و سپس هدر
        debtDetailRepository.deleteByDebtHeader_Id(id);
        debtHeaderRepository.delete(h);
    }

    // ---------------- VIEW ----------------

    @Transactional(readOnly = true)
    public DebtViewResponse view(Long debtId) {
        if (debtId == null) throw new IllegalArgumentException("شناسه بدهی الزامی است.");

        DebtHeader h = debtHeaderRepository.findById(debtId)
                .orElseThrow(() -> new IllegalArgumentException("بدهی مورد نظر یافت نشد. (شناسه: " + debtId + ")"));

        List<DebtLineResponse> lines = loadDebtLinesWithTitles(debtId);

        BigDecimal total = lines.stream()
                .map(DebtLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal covered = trackRepository.sumCoveredByDebt(debtId);
        if (covered == null) covered = BigDecimal.ZERO;

        BigDecimal remaining = total.subtract(covered);

        List<DebtAllocationView> allocations = loadAllocationsView(debtId);

        return new DebtViewResponse(
                toHeaderResponse(h),
                lines,
                allocations,
                total,
                covered,
                remaining
        );
    }

    // ---------------- "Open debts" listing ----------------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> openDebts(Long projectId, Long personId) {
        if (projectId == null) throw new IllegalArgumentException("شناسه پروژه الزامی است.");

        String sql = """
            select
                dh.id as debt_id,
                dh.project_id,
                dh.person_id,
                dh.date_due,
                dh.date_registered,
                coalesce(sum(cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))),0) as total_amount,
                coalesce(tt.covered,0) as covered_amount,
                (coalesce(sum(cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))),0) - coalesce(tt.covered,0)) as remaining_amount
            from debts_header dh
            join debts_detail dd on dd.debt_header_id = dh.id
            left join (
                select debt_header_id, coalesce(sum(covered_amount),0) as covered
                from transaction_tracks
                group by debt_header_id
            ) tt on tt.debt_header_id = dh.id
            where dh.project_id = ?
              and (? is null or dh.person_id = ?)
            group by dh.id, dh.project_id, dh.person_id, dh.date_due, dh.date_registered, tt.covered
            having (coalesce(sum(cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))),0) - coalesce(tt.covered,0)) > 0
            order by dh.date_registered desc, dh.id desc
            """;

        return jdbcTemplate.queryForList(sql, projectId, personId, personId);
    }

    // ---------------- ALL DEBTS ----------------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllDebts() {

        String sql = """
        select
            dh.id as debt_id,
            dh.project_id,
            dh.person_id,
            dh.date_registered,
            dh.date_due,

            coalesce(
                sum(cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))),
                0
            ) as total_amount,

            coalesce(tt.covered, 0) as covered_amount,

            (
                coalesce(
                    sum(cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))),
                    0
                ) - coalesce(tt.covered, 0)
            ) as remaining_amount

        from debts_header dh
        join debts_detail dd
            on dd.debt_header_id = dh.id

        left join (
            select
                debt_header_id,
                coalesce(sum(covered_amount), 0) as covered
            from transaction_tracks
            group by debt_header_id
        ) tt on tt.debt_header_id = dh.id

        group by
            dh.id,
            dh.project_id,
            dh.person_id,
            dh.date_registered,
            dh.date_due,
            tt.covered

        order by dh.date_registered desc, dh.id desc
        """;

        return jdbcTemplate.queryForList(sql);
    }

    // ---------------- helpers ----------------

    private void applyHeader(DebtHeader header,
                             Long projectId,
                             Long personId,
                             java.time.LocalDate dateDue,
                             java.time.LocalDateTime dateRegistered,
                             String dsc) {

        if (projectId == null) throw new IllegalArgumentException("پروژه الزامی است.");
        if (personId == null) throw new IllegalArgumentException("شخص الزامی است.");
        if (dateDue == null) throw new IllegalArgumentException("تاریخ سررسید الزامی است.");
        if (dateRegistered == null) throw new IllegalArgumentException("تاریخ ثبت الزامی است.");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("پروژه مورد نظر یافت نشد. (شناسه: " + projectId + ")"));

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("شخص مورد نظر یافت نشد. (شناسه: " + personId + ")"));

        header.setProject(project);
        header.setPerson(person);
        header.setDateDue(dateDue);
        header.setDateRegistered(dateRegistered);
        header.setDsc(trimToNull(dsc));
    }

    private void saveLines(DebtHeader header, List<DebtLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("حداقل یک ردیف برای بدهی الزامی است.");
        }

        // جلوگیری از تکراری بودن کالا/خدمت در درخواست
        Set<Long> itemIds = new HashSet<>();
        for (DebtLineRequest l : lines) {
            if (l == null) throw new IllegalArgumentException("یک ردیف نامعتبر در لیست ردیف‌ها وجود دارد.");
            if (l.itemId() == null) throw new IllegalArgumentException("کالا/خدمت در ردیف بدهی الزامی است.");
            if (!itemIds.add(l.itemId())) {
                throw new IllegalArgumentException("کالا/خدمت در ردیف‌های بدهی تکراری است. (شناسه کالا/خدمت: " + l.itemId() + ")");
            }
            if (l.unitId() == null) throw new IllegalArgumentException("واحد در ردیف بدهی الزامی است.");
            if (l.qnt() == null || l.qnt().compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("مقدار (qnt) باید بزرگتر از صفر باشد.");
            if (l.unitPrice() == null || l.unitPrice().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("قیمت واحد نمی‌تواند منفی باشد.");
        }

        for (DebtLineRequest l : lines) {
            Item item = itemRepository.findById(l.itemId())
                    .orElseThrow(() -> new IllegalArgumentException("کالا/خدمت مورد نظر یافت نشد. (شناسه: " + l.itemId() + ")"));

            Unit unit = unitRepository.findById(l.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("واحد مورد نظر یافت نشد. (شناسه: " + l.unitId() + ")"));

            DebtDetail d = new DebtDetail();
            d.setDebtHeader(header);
            d.setItem(item);
            d.setUnit(unit);
            d.setQnt(l.qnt());
            d.setUnitPrice(l.unitPrice());
            d.setDsc(trimToNull(l.dsc()));
            debtDetailRepository.save(d);
        }
    }

    private BigDecimal calcTotalFromLines(List<DebtLineRequest> lines) {
        return lines.stream()
                .map(l -> l.qnt().multiply(l.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DebtHeaderResponse toHeaderResponse(DebtHeader h) {
        return new DebtHeaderResponse(
                h.getId(),
                h.getProject().getId(),
                h.getPerson().getId(),
                h.getDateDue(),
                h.getDateRegistered(),
                h.getDsc()
        );
    }

    private List<DebtLineResponse> loadDebtLinesWithTitles(Long debtId) {
        String sql = """
            select
                dd.id as id,
                dd.item_id as item_id,
                i.title as item_title,
                dd.unit_id as unit_id,
                u.title as unit_title,
                dd.qnt as qnt,
                dd.unit_price as unit_price,
                (cast(dd.qnt as decimal(18,3)) * cast(dd.unit_price as decimal(18,0))) as line_total,
                dd.dsc as dsc
            from debts_detail dd
            join items i on i.id = dd.item_id
            join units u on u.id = dd.unit_id
            where dd.debt_header_id = ?
            order by dd.id asc
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DebtLineResponse(
                rs.getLong("id"),
                rs.getLong("item_id"),
                rs.getString("item_title"),
                rs.getLong("unit_id"),
                rs.getString("unit_title"),
                rs.getBigDecimal("qnt"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("line_total"),
                rs.getString("dsc")
        ), debtId);
    }

    private List<DebtAllocationView> loadAllocationsView(Long debtId) {
        String sql = """
            select
                tt.id as allocation_id,
                tt.transaction_id,
                t.code as transaction_code,
                t.date_registered as transaction_date_registered,
                t.amount_paid as transaction_amount_paid,
                tt.covered_amount,
                tt.dsc
            from transaction_tracks tt
            join transactions t on t.id = tt.transaction_id
            where tt.debt_header_id = ?
            order by tt.id desc
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DebtAllocationView(
                rs.getLong("allocation_id"),
                rs.getLong("transaction_id"),
                rs.getString("transaction_code"),
                rs.getDate("transaction_date_registered").toLocalDate(),
                rs.getBigDecimal("transaction_amount_paid"),
                rs.getBigDecimal("covered_amount"),
                rs.getString("dsc")
        ), debtId);
    }

    private boolean hasDebtDocuments(Long debtId) {
        Integer cnt = jdbcTemplate.queryForObject(
                "select count(1) from debts_documents where debt_header_id = ?",
                Integer.class,
                debtId
        );
        return cnt != null && cnt > 0;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }
}
