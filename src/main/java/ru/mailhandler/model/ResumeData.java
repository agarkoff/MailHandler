package ru.mailhandler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeData {

    private String firstName = "";
    private String lastName = "";
    private String email = "";
    private String phone = "";
    private String location = "";

    public boolean isNotBlank() {
        return StringUtils.isNotBlank(firstName) ||
                StringUtils.isNotBlank(lastName) ||
                StringUtils.isNotBlank(email) ||
                StringUtils.isNotBlank(phone) ||
                StringUtils.isNotBlank(location);
    }
}
