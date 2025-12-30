
package com.app.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {

    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
