package study.data_jpa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimpleMemberDto {
    private String username;
    private int age;

    public SimpleMemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
