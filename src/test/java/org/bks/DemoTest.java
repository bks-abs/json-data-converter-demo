package org.bks;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.bks.po.ConvConfig;
import org.bks.po.DstField;
import org.bks.po.FieldType;
import org.bks.po.SrcField;
import org.bks.util.Src2DstUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.bks.util.Src2DstUtil.DST_ROOT_FIELD_NAME;
import static org.bks.util.Src2DstUtil.SRC_ROOT_FIELD_NAME;


public class DemoTest {


    @Test
    public void testOnlineApiStructure() throws JsonProcessingException {

        String src = """
                {
                     "a": {
                         "b": [
                             {
                                 "c": {
                                     "d": {
                                         "e": [
                                             null,
                                             -1,
                                             4
                                         ]
                                     },
                                     "f": [
                                         -112,
                                         -44
                                     ]
                                 }
                             }
                         ]
                     }
                 }
                """;


        SrcField srcRoot = new SrcField("0", "-1", SRC_ROOT_FIELD_NAME, FieldType.OBJECT);
        SrcField a = new SrcField("1", srcRoot.getId(), "a", FieldType.OBJECT);
        SrcField b = new SrcField("2", a.getId(), "b", FieldType.ARRAY);
        SrcField b_element = new SrcField("3", b.getId(), "b_element", FieldType.OBJECT);
        SrcField c = new SrcField("4", b_element.getId(), "c", FieldType.OBJECT);
        SrcField d = new SrcField("5", c.getId(), "d", FieldType.OBJECT);
        SrcField e = new SrcField("6", d.getId(), "e", FieldType.ARRAY);
        SrcField e_element = new SrcField("7", e.getId(), "e_element", FieldType.INTEGER);
        SrcField f = new SrcField("8", c.getId(), "f", FieldType.ARRAY);
        SrcField f_element = new SrcField("9", f.getId(), "f_element", FieldType.INTEGER);
        // config child
        srcRoot.addChild(a);
        a.addChild(b);
        b.addChild(b_element);
        b_element.addChild(c);
        c.addChild(d, f);
        d.addChild(e);
        e.addChild(e_element);
        f.addChild(f_element);


        String dst = """
                {
                  "__DST__": {
                    "x1": {
                      "x2": [
                        {
                          "x3": [
                            null,
                            -1,
                            4
                          ]
                        }
                      ]
                    },
                    "x4": {
                      "x5": [
                        {
                          "x5": [
                            -112,
                            -44
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        DstField dstRoot = new DstField("0", "-1", DST_ROOT_FIELD_NAME, FieldType.OBJECT);
        DstField x1 = new DstField("1", dstRoot.getId(), "x1", FieldType.OBJECT);
        DstField x4 = new DstField("2", dstRoot.getId(), "x4", FieldType.OBJECT);
        DstField x2 = new DstField("3", x1.getId(), "x2", FieldType.ARRAY);
        DstField x2_element = new DstField("4", x2.getId(), "x2_element", FieldType.OBJECT);
        DstField x5 = new DstField("5", x4.getId(), "x5", FieldType.ARRAY);
        DstField x2_x3 = new DstField("6", x2_element.getId(), "x3", FieldType.ARRAY);
        DstField x2_x3_element = new DstField("7", x2.getId(), "x3_element", FieldType.INTEGER);
        DstField x5_element = new DstField("8", x5.getId(), "x5_element", FieldType.OBJECT);
        DstField x5_x5 = new DstField("9", x5_element.getId(), "x5", FieldType.ARRAY);
        DstField x5_x5_element = new DstField("10", x5_x5.getId(), "x5_x5_element", FieldType.INTEGER);
        dstRoot.addChild(x1, x4);
        x1.addChild(x2);
        x2.addChild(x2_element);
        x2_element.addChild(x2_x3);
        x2_x3.addChild(x2_x3_element);
        x4.addChild(x5);
        x5.addChild(x5_element);
        x5_element.addChild(x5_x5);
        x5_x5.addChild(x5_x5_element);

        // build config
        List<ConvConfig> convConfigs = new ArrayList<>();
        convConfigs.add(new ConvConfig(e_element.getId(), x2_x3_element.getId()));
        convConfigs.add(new ConvConfig(f_element.getId(), x5_x5_element.getId()));

        JsonNode jsonNode = Src2DstUtil.convertJson(srcRoot, dstRoot, convConfigs, src);
        System.out.println(jsonNode);

    }

    @Test
    public void testMultiLevelArrayStructure() throws JsonProcessingException {
        String src = """
                {
                    "data": {
                        "items": [
                            {
                                "scores": [
                                    [90, 85, 88],
                                    [92, 87, 91]
                                ],
                                "name": "Student1"
                            },
                            {
                                "scores": [
                                    [78, 82, 80],
                                    [85, 88, 84]
                                ],
                                "name": "Student2"
                            }
                        ]
                    }
                }
                """;

        // 构建源结构
        SrcField srcRoot = new SrcField("0", "-1", SRC_ROOT_FIELD_NAME, FieldType.OBJECT);
        SrcField data = new SrcField("1", srcRoot.getId(), "data", FieldType.OBJECT);
        SrcField items = new SrcField("2", data.getId(), "items", FieldType.ARRAY);
        SrcField items_element = new SrcField("3", items.getId(), "items_element", FieldType.OBJECT);
        SrcField scores = new SrcField("4", items_element.getId(), "scores", FieldType.ARRAY);
        SrcField scores_row = new SrcField("5", scores.getId(), "scores_row", FieldType.ARRAY);
        SrcField scores_element = new SrcField("6", scores_row.getId(), "scores_element", FieldType.INTEGER);
        SrcField name = new SrcField("7", items_element.getId(), "name", FieldType.STRING);

        srcRoot.addChild(data);
        data.addChild(items);
        items.addChild(items_element);
        items_element.addChild(scores, name);
        scores.addChild(scores_row);
        scores_row.addChild(scores_element);

        // 构建目标结构
        String dst = """
                {
                    "__DST__": {
                        "results": [
                            {
                                "grades": [
                                    [90, 85, 88],
                                    [92, 87, 91]
                                ],
                                "studentName": "Student1"
                            },
                            {
                                "grades": [
                                    [78, 82, 80],
                                    [85, 88, 84]
                                ],
                                "studentName": "Student2"
                            }
                        ]
                    }
                }
                """;

        DstField dstRoot = new DstField("0", "-1", DST_ROOT_FIELD_NAME, FieldType.OBJECT);
        DstField results = new DstField("1", dstRoot.getId(), "results", FieldType.ARRAY);
        DstField results_element = new DstField("2", results.getId(), "results_element", FieldType.OBJECT);
        DstField grades = new DstField("3", results_element.getId(), "grades", FieldType.ARRAY);
        DstField grades_row = new DstField("4", grades.getId(), "grades_row", FieldType.ARRAY);
        DstField grades_element = new DstField("5", grades_row.getId(), "grades_element", FieldType.INTEGER);
        DstField studentName = new DstField("6", results_element.getId(), "studentName", FieldType.STRING);

        dstRoot.addChild(results);
        results.addChild(results_element);
        results_element.addChild(grades, studentName);
        grades.addChild(grades_row);
        grades_row.addChild(grades_element);

        // 构建转换配置
        List<ConvConfig> convConfigs = new ArrayList<>();
        convConfigs.add(new ConvConfig(scores_element.getId(), grades_element.getId()));
        convConfigs.add(new ConvConfig(name.getId(), studentName.getId()));

        JsonNode jsonNode = Src2DstUtil.convertJson(srcRoot, dstRoot, convConfigs, src);
        System.out.println(jsonNode);
    }

    @Test
    public void testScatteredArraysStructure() throws JsonProcessingException {
        String src = """
            {
                "catalog": {
                    "products": [
                        [
                            {
                                "id": 101,
                                "name": "Laptop",
                                "price": 999.99
                            },
                            {
                                "id": 102,
                                "name": "Smartphone",
                                "price": 699.99
                            }
                        ],
                        [
                            {
                                "id": 201,
                                "name": "Tablet",
                                "price": 349.99
                            },
                            {
                                "id": 202,
                                "name": "Smartwatch",
                                "price": 249.99
                            }
                        ]
                    ]
                }
            }
            """;

        // 构建源结构 - 紧挨着的数组嵌套 [[]]
        SrcField srcRoot = new SrcField("0", "-1", SRC_ROOT_FIELD_NAME, FieldType.OBJECT);
        SrcField catalog = new SrcField("1", srcRoot.getId(), "catalog", FieldType.OBJECT);
        SrcField products = new SrcField("2", catalog.getId(), "products", FieldType.ARRAY);
        SrcField category_array = new SrcField("3", products.getId(), "category_array", FieldType.ARRAY);
        SrcField product = new SrcField("4", category_array.getId(), "product", FieldType.OBJECT);
        SrcField id = new SrcField("5", product.getId(), "id", FieldType.INTEGER);
        SrcField name = new SrcField("6", product.getId(), "name", FieldType.STRING);
        SrcField price = new SrcField("7", product.getId(), "price", FieldType.DOUBLE);

        srcRoot.addChild(catalog);
        catalog.addChild(products);
        products.addChild(category_array);
        category_array.addChild(product);
        product.addChild(id, name, price);

        // 构建目标结构 - 分散的数组嵌套 [{"a":[]}]
        String dst = """
            {
                "__DST__": {
                    "categories": [
                        {
                            "categoryId": 1,
                            "items": [
                                {
                                    "productId": 101,
                                    "productName": "Laptop",
                                    "cost": 999.99
                                },
                                {
                                    "productId": 102,
                                    "productName": "Smartphone",
                                    "cost": 699.99
                                }
                            ]
                        },
                        {
                            "categoryId": 2,
                            "items": [
                                {
                                    "productId": 201,
                                    "productName": "Tablet",
                                    "cost": 349.99
                                },
                                {
                                    "productId": 202,
                                    "productName": "Smartwatch",
                                    "cost": 249.99
                                }
                            ]
                        }
                    ]
                }
            }
            """;

        DstField dstRoot = new DstField("0", "-1", DST_ROOT_FIELD_NAME, FieldType.OBJECT);
        DstField categories = new DstField("1", dstRoot.getId(), "categories", FieldType.ARRAY);
        DstField category = new DstField("2", categories.getId(), "category", FieldType.OBJECT);
        DstField categoryId = new DstField("3", category.getId(), "categoryId", FieldType.INTEGER);
        DstField items = new DstField("4", category.getId(), "items", FieldType.ARRAY);
        DstField item = new DstField("5", items.getId(), "item", FieldType.OBJECT);
        DstField productId = new DstField("6", item.getId(), "productId", FieldType.INTEGER);
        DstField productName = new DstField("7", item.getId(), "productName", FieldType.STRING);
        DstField cost = new DstField("8", item.getId(), "cost", FieldType.DOUBLE);

        dstRoot.addChild(categories);
        categories.addChild(category);
        category.addChild(categoryId, items);
        items.addChild(item);
        item.addChild(productId, productName, cost);

        // 构建转换配置
        List<ConvConfig> convConfigs = new ArrayList<>();
        convConfigs.add(new ConvConfig(id.getId(), productId.getId()));
        convConfigs.add(new ConvConfig(name.getId(), productName.getId()));
        convConfigs.add(new ConvConfig(price.getId(), cost.getId()));

        JsonNode jsonNode = Src2DstUtil.convertJson(srcRoot, dstRoot, convConfigs, src);
        System.out.println(jsonNode);
    }

    @Test
    public void testNestedArrayInObject() throws JsonProcessingException {
        String src = """
                {
                    "a": {
                        "b": {
                            "c": [
                                {
                                    "id": 101,
                                    "name": "Product A",
                                    "price": 99.99
                                },
                                {
                                    "id": 102, 
                                    "name": "Product B",
                                    "price": 199.99
                                }
                            ],
                            "d": {
                                "e": "some value"
                            }
                        }
                    }
                }
                """;

        String dst = """
                {
                  "__DST__": {
                    "products": [
                      {
                        "itemId": 101,
                        "itemName": "Product A",
                        "itemPrice": 99.99
                      },
                      {
                        "itemId": 102,
                        "itemName": "Product B",
                        "itemPrice": 199.99
                      }
                    ]
                  }
                }
                
                """;

        SrcField srcRoot = new SrcField("0", "-1", SRC_ROOT_FIELD_NAME, FieldType.OBJECT);
        SrcField a = new SrcField("1", srcRoot.getId(), "a", FieldType.OBJECT);
        SrcField b = new SrcField("2", a.getId(), "b", FieldType.OBJECT);
        SrcField c = new SrcField("3", b.getId(), "c", FieldType.ARRAY);
        SrcField product = new SrcField("4", c.getId(), "product", FieldType.OBJECT);
        SrcField id = new SrcField("5", product.getId(), "id", FieldType.INTEGER);
        SrcField name = new SrcField("6", product.getId(), "name", FieldType.STRING);
        SrcField price = new SrcField("7", product.getId(), "price", FieldType.DOUBLE);

        srcRoot.addChild(a);
        a.addChild(b);
        b.addChild(c);
        c.addChild(product);
        product.addChild(id, name, price);

        DstField dstRoot = new DstField("0", "-1", DST_ROOT_FIELD_NAME, FieldType.OBJECT);
        DstField products = new DstField("1", dstRoot.getId(), "products", FieldType.ARRAY);
        DstField item = new DstField("2", products.getId(), "item", FieldType.OBJECT);
        DstField itemId = new DstField("3", item.getId(), "itemId", FieldType.INTEGER);
        DstField itemName = new DstField("4", item.getId(), "itemName", FieldType.STRING);
        DstField itemPrice = new DstField("5", item.getId(), "itemPrice", FieldType.DOUBLE);

        dstRoot.addChild(products);
        products.addChild(item);
        item.addChild(itemId, itemName, itemPrice);

        List<ConvConfig> convConfigs = new ArrayList<>();
        convConfigs.add(new ConvConfig(id.getId(), itemId.getId()));
        convConfigs.add(new ConvConfig(name.getId(), itemName.getId()));
        convConfigs.add(new ConvConfig(price.getId(), itemPrice.getId()));

        JsonNode jsonNode = Src2DstUtil.convertJson(srcRoot, dstRoot, convConfigs, src);
        System.out.println(jsonNode);
    }
}
