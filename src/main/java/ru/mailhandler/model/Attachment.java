package ru.mailhandler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString(exclude = {"data"})
public class Attachment {

    private String filename;
    private byte[] data;
}
