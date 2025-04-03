package org.bks.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 源数据字段
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrcField {

    private String id;

    private String pid;

    private String name;

    private FieldType type;

    private SrcField parent;

    private List<SrcField> children;

    public SrcField(String id, String pid, String name, FieldType type) {
        this.id = id;
        this.pid = pid;
        this.name = name;
        this.type = type;

    }


    public void addChild(SrcField... srcFields) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        Collections.addAll(this.children, srcFields);
        Arrays.stream(srcFields).forEach(f -> f.setParent(this));
    }

    @Override
    public String toString() {
        return "SrcField{" +
                "id='" + id + '\'' +
                ", pid='" + pid + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", parent=" + (parent == null ? null : parent.getName()) +
                '}';
    }

}
