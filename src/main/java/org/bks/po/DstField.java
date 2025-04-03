package org.bks.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 目标数据字段
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DstField {

    private String id;

    private String pid;

    private String name;

    private FieldType type;

    private DstField parent;


    private List<DstField> children;


    public DstField(String id, String pid, String name, FieldType type) {
        this.id = id;
        this.pid = pid;
        this.name = name;
        this.type = type;
    }


    public void addChild(DstField... dstFields) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        Collections.addAll(this.children, dstFields);
        Arrays.stream(dstFields).forEach(f -> f.setParent(this));
    }


    @Override
    public String toString() {
        return "DstField{" +
                "id='" + id + '\'' +
                ", pid='" + pid + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", parent=" + (parent == null ? null : parent.getName()) +
                '}';
    }

}
