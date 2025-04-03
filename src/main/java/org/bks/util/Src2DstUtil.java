package org.bks.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bks.po.ConvConfig;
import org.bks.po.DstField;
import org.bks.po.FieldType;
import org.bks.po.SrcField;

import java.util.*;

public class Src2DstUtil {

    private static final ObjectMapper OM = JacksonUtil.getInstance();
    public static final String SRC_ROOT_FIELD_NAME = "__SRC__";
    public static final String DST_ROOT_FIELD_NAME = "__DST__";

    public static JsonNode DEBUG_NODE;

    public static JsonNode convertJson(SrcField srcRootField, DstField dstRootField,
                                       List<ConvConfig> convConfigs,
                                       String sourceJson) throws JsonProcessingException {
        Map<String, SrcField> src_id2Field = flatSrcField(srcRootField);
        Map<String, DstField> dst_id2Field = flatDstField(dstRootField);

        // 以目标叶子节点字段进行转换
        Map<String, ConvConfig> dstId2ConvConfig = new HashMap<>();
        for (ConvConfig convConfig : convConfigs) {
            dstId2ConvConfig.put(convConfig.getDstId(), convConfig);
        }

        ObjectNode srcRootNode = OM.createObjectNode();
        srcRootNode.put(SRC_ROOT_FIELD_NAME, OM.readTree(sourceJson));
        ObjectNode dstRootNode = OM.createObjectNode();

        DEBUG_NODE = dstRootNode; // TODO 用于调试


        dstId2ConvConfig.forEach((dstId, convConfig) -> {
            DstField dstLeafField = dst_id2Field.get(convConfig.getDstId());
            SrcField srcLeafField = src_id2Field.get(convConfig.getSrcId());

            // dst叶子节点的路径(从根节点到叶子节点)
            List<DstField> dstPath = getDstPathFromRoot(dstLeafField);
            List<SrcField> srcPath = getSrcPathFromRoot(srcLeafField);
            int[] pos = initArrayPos(dstLeafField);

            convertLeafNode(dstRootNode,
                    dstPath, srcPath, 0,
                    pos, 0,
                    srcRootNode,
                    0);
        });

        return dstRootNode;
    }

    /**
     * 通过遍历目标字段从根节点到叶子节点的路径,逐层创建JsonNode,直到遇到基本类型,才设置元素的值
     *
     * @param dstParentNode 当前被处理的JsonNode节点的父节点
     * @param dstPath       目标字段从根节点到叶子节点的路径
     * @param srcPath       对应的源字段从根节点到叶子节点的路径
     * @param dstPathIndex  当前处理到的目标字段路径的索引
     * @param pos
     * @param arrayDepth    第几个数组类型的字段
     * @param srcRootNode   源数据的根节点
     * @param k
     */
    private static void convertLeafNode(JsonNode dstParentNode,
                                        List<DstField> dstPath, List<SrcField> srcPath, int dstPathIndex,
                                        int[] pos, int arrayDepth,
                                        JsonNode srcRootNode,
                                        int k) {
        if (dstPathIndex >= dstPath.size()) {
            return;
        }

        DstField dstField = dstPath.get(dstPathIndex);

        if (FieldType.isBasicType(dstField.getType())) {
            setBasicValue(srcRootNode, srcPath, dstField, dstParentNode, dstPathIndex, pos);
        } else if (FieldType.OBJECT == dstField.getType()) {
            ObjectNode curJsonNode = getOrCreateObjNode(dstParentNode, dstField.getName(), k); // 获取指定位置的索引
            convertLeafNode(curJsonNode, dstPath, srcPath, dstPathIndex + 1, pos, arrayDepth, srcRootNode, k);
        } else if (FieldType.ARRAY == dstField.getType()) {

            int d = getArrayDim(srcRootNode, srcPath, pos, arrayDepth);
            FieldType arrayElementType = dstPath.get(dstPathIndex + 1).getType();
            ArrayNode arrayNode = getOrCreateArrayNode(dstParentNode, arrayElementType, dstField.getName(), d, k);

            for (int i = 0; i < d; i++) {
                pos[arrayDepth] = i;
                convertLeafNode(arrayNode,
                        dstPath, srcPath, dstPathIndex + 1, pos, arrayDepth + 1, srcRootNode, i);
                pos[arrayDepth] = -1;
            }
        }
    }

    /**
     * 根据路径和数组位置遍历源数据
     *
     * @param srcRootNode
     * @param srcPath
     * @param dstParentNode
     * @param dstPathIndex
     * @param pos
     */
    private static void setBasicValue(JsonNode srcRootNode, List<SrcField> srcPath,
                                      DstField dstField,
                                      JsonNode dstParentNode, int dstPathIndex, int[] pos) {
        int curArrayDepth = 0;
        String value = null;
        JsonNode cur = srcRootNode;
        int pathIndex = 0;
        for (int i = 0; i < srcPath.size(); i++) {

            SrcField srcField = srcPath.get(i);
            FieldType type = srcField.getType();
            if (type == FieldType.ARRAY) {
                if (isNullNode(cur)) {
                    value = null;
                    break;
                }

                // 根据父节点是数组还是对象来进行取值
                if (cur instanceof ObjectNode) {
                    cur = cur.get(srcPath.get(pathIndex++).getName());
                } else if (cur instanceof ArrayNode) {
                    cur = cur.get(pos[curArrayDepth++]);
                    pathIndex++;
                } else {
                    throw new IllegalStateException("非法类型");
                }

            } else if (type == FieldType.OBJECT) {
                if (isNullNode(cur)) {
                    value = null;
                    break;
                }

                // 根据父节点是数组还是对象来进行取值
                if (cur instanceof ObjectNode) {
                    cur = cur.get(srcPath.get(pathIndex++).getName());
                } else if (cur instanceof ArrayNode) {
                    cur = cur.get(pos[curArrayDepth++]);
                    pathIndex++;
                } else {
                    throw new IllegalStateException("非法类型");
                }
            } else if (FieldType.isBasicType(type)) {
                if (isNullNode(cur)) {
                    value = null;
                    break;
                }

                // 根据父节点是数组还是对象来进行取值
                if (cur instanceof ObjectNode) {
                    cur = cur.get(srcPath.get(pathIndex++).getName());
                } else if (cur instanceof ArrayNode) {
                    cur = cur.get(pos[curArrayDepth++]);
                    pathIndex++;
                } else {
                    throw new IllegalStateException("非法类型");
                }

                value = cur.asText(null);
                break;
            }
        }

        if (dstParentNode instanceof ArrayNode) {
            FieldType type = dstField.getType();
            ArrayNode p = (ArrayNode) dstParentNode;
            int index = pos[pos.length - 1];
            if (type == FieldType.BOOLEAN) {
                p.add(Boolean.valueOf(value));
            } else if (type == FieldType.STRING) {
                p.add(value);
            } else if (type == FieldType.INTEGER) {
                p.add( parseLongOrThrowEx(value, "*"));
            } else if (type == FieldType.DOUBLE) {
                p.add( parseDoubleOrThrowEx(value, "*"));
            } else {
                throw new IllegalStateException("非法数据类型");
            }
        } else if (dstParentNode instanceof ObjectNode) {
            String name = dstField.getName();
            FieldType type = dstField.getType();
            ObjectNode p = (ObjectNode) dstParentNode;
            if (type == FieldType.BOOLEAN) {
                p.put(name, Boolean.valueOf(value));
            } else if (type == FieldType.STRING) {
                p.put(name, value);
            } else if (type == FieldType.INTEGER) {
                p.put(name, parseLongOrThrowEx(value, name));
            } else if (type == FieldType.DOUBLE) {
                p.put(name, parseDoubleOrThrowEx(value, name));
            } else {
                throw new IllegalStateException("非法数据类型");
            }
        } else {
            throw new IllegalStateException("非法数据类型");
        }

    }


    /**
     * 从源数据的数组推断成目标数据当前数组的维度
     *
     * @param srcRootNode
     * @param srcPath
     * @param pos
     * @param arrayDepth  表示路径上的N个深度维度,遍历到什么时候截止
     * @return
     */
    private static int getArrayDim(JsonNode srcRootNode, List<SrcField> srcPath,
                                   int[] pos, int arrayDepth) {

        int curArrayDepth = 0;
        JsonNode cur = srcRootNode;
        JsonNode prev = null;
        int pathIndex = 0;
        for (int i = 0; i < srcPath.size(); i++) {

            SrcField srcField = srcPath.get(i);
            FieldType type = srcField.getType();
            String name = srcField.getName();

            if (isNullNode(cur)) {
                return 0; // 说明源数据此时为NULL了
            }


            if (type == FieldType.ARRAY) {
                // 根据父节点是数组还是对象来进行取值
                if (cur instanceof ObjectNode) {
                    cur = cur.get(srcPath.get(pathIndex++).getName());
                } else if (cur instanceof ArrayNode) {
                    cur = cur.get(pos[curArrayDepth++]);
                    pathIndex++;
                } else {
                    throw new IllegalStateException("非法类型");
                }

                if (curArrayDepth == arrayDepth) {
                    ArrayNode n = (ArrayNode) cur;
                    return n.size();
                }


            } else if (type == FieldType.OBJECT) {
                // 根据父节点是数组还是对象来进行取值
                if (cur instanceof ObjectNode) {
                    cur = cur.get(srcPath.get(pathIndex++).getName());
                } else if (cur instanceof ArrayNode) {
                    cur = cur.get(pos[curArrayDepth++]);
                    pathIndex++;
                } else {
                    throw new IllegalStateException("非法类型");
                }
            } else {
                throw new IllegalStateException("非法类型");
            }
        }

        throw new IllegalStateException("无法获取维度");
    }


    /**
     * 给父节点添加一个对象节点
     *
     * @param parentNode 父节点
     * @param name       仅当父节点为对象时需要,因为父节点为数组时,数组里面的元素是不应该有名字存在的
     * @param index      当父节点为数组类型时,该索引表面的是在第N个位置的数组元素添加子节点
     * @return 返回添加的或者既有的对象节点
     */
    // index: 在父节点为数组类型时的索引位置
    private static ObjectNode getOrCreateObjNode(JsonNode parentNode, String name, int index) {
        if (parentNode instanceof ObjectNode) {
            ObjectNode p = (ObjectNode) parentNode;
            if (!p.has(name)) {
                p.set(name, OM.createObjectNode());
            }
            JsonNode r = p.get(name);
            return (ObjectNode) r;
        } else if (parentNode instanceof ArrayNode) {
            ArrayNode p = (ArrayNode) parentNode;
            JsonNode r = p.get(index);
            if (r == null) {
                r = OM.createObjectNode();
                p.set(index, r);
            }
            return (ObjectNode) r;
        } else {
            throw new IllegalStateException("类型异常");
        }
    }

    /**
     * 给父节点添加一个数组节点
     *
     * @param parentNode 父节点
     * @param name       仅当父节点为对象时需要,因为父节点为数组时,数组里面的元素是不应该有名字存在的
     * @param index      当父节点为数组类型时,该索引表面的是在第N个位置的数组元素添加子节点
     * @return 返回添加的或者既有的子节点
     */
    private static ArrayNode getOrCreateArrayNode(JsonNode parentNode, FieldType arrayElementType,
                                                  String name, int size,
                                                  int index) {
        if (parentNode instanceof ObjectNode) {
            ObjectNode p = (ObjectNode) parentNode;
            if (!p.has(name)) {
                ArrayNode arrayNode = OM.createArrayNode();
                for (int i = 0; i < size; i++) {
                    // only object node should create necessary
                    if (arrayElementType == FieldType.OBJECT) {
                        arrayNode.add(OM.createObjectNode());
                    } else if (arrayElementType == FieldType.ARRAY) {
                        arrayNode.add(OM.createArrayNode());
                    } else if (arrayElementType == FieldType.BOOLEAN) {
                        arrayNode.add((Boolean) null);
                    } else if (arrayElementType == FieldType.DOUBLE) {
                        arrayNode.add((Double) null);
                    } else if (arrayElementType == FieldType.INTEGER) {
                        arrayNode.add((Long) null);
                    } else if (arrayElementType == FieldType.STRING) {
                        arrayNode.add((String) null);
                    } else {
                        throw new UnsupportedOperationException("不支持的类型");
                    }
                }
                p.set(name, arrayNode);
            }

            JsonNode jsonNode = p.get(name);
            return (ArrayNode) jsonNode;
        } else if (parentNode instanceof ArrayNode) {
            ArrayNode p = (ArrayNode) parentNode;
            JsonNode r = p.get(index);
            if (r == null) {
                ArrayNode arrayNode = OM.createArrayNode();
                for (int i = 0; i < size; i++) {
                    // only object node should create necessary
                    if (arrayElementType == FieldType.OBJECT) {
                        arrayNode.add(OM.createObjectNode());
                    } else if (arrayElementType == FieldType.ARRAY) {
                        arrayNode.add(OM.createArrayNode());
                    } else if (arrayElementType == FieldType.BOOLEAN) {
                        arrayNode.add((Boolean) null);
                    } else if (arrayElementType == FieldType.DOUBLE) {
                        arrayNode.add((Double) null);
                    } else if (arrayElementType == FieldType.INTEGER) {
                        arrayNode.add((Long) null);
                    } else if (arrayElementType == FieldType.STRING) {
                        arrayNode.add((String) null);
                    } else {
                        throw new UnsupportedOperationException("不支持的类型");
                    }
                }
                p.set(index, arrayNode);
                r = p.get(index);
            }
            return (ArrayNode) r;
        } else {
            throw new IllegalStateException("类型异常");
        }
    }


    private static int[] initArrayPos(DstField dstLeafField) {
        int d = 0;
        for (DstField cur = dstLeafField; ; ) {
            cur = cur.getParent();
            if (cur != null) {
                if (cur.getType() == FieldType.ARRAY) {
                    d++;
                }
            } else {
                break;
            }
        }
        int[] pos = new int[d];
        Arrays.fill(pos, -1);
        return pos;
    }

    private static List<DstField> getDstPathFromRoot(DstField dstLeafField) {
        List<DstField> paths = new ArrayList<>();
        paths.add(0, dstLeafField);
        for (DstField cur = dstLeafField; ; ) {
            cur = cur.getParent();
            if (cur != null) {
                paths.add(0, cur);
            } else {
                break;
            }
        }
        return paths;
    }


    private static List<SrcField> getSrcPathFromRoot(SrcField srcLeafField) {
        List<SrcField> paths = new ArrayList<>();
        paths.add(0, srcLeafField);
        for (SrcField cur = srcLeafField; ; ) {
            cur = cur.getParent();
            if (cur != null) {
                paths.add(0, cur);
            } else {
                break;
            }
        }
        return paths;
    }

    public static Map<String, SrcField> flatSrcField(SrcField srcRoot) {
        Map<String, SrcField> r = new HashMap<>();

        // bfs
        Queue<SrcField> q = new LinkedList<>();
        q.add(srcRoot);
        r.put(srcRoot.getId(), srcRoot);

        while (!q.isEmpty()) {

            int size = q.size();
            for (int i = 0; i < size; i++) {

                SrcField field = q.poll();
                r.put(field.getId(), field);

                List<SrcField> nextLevelFields = field.getChildren();
                if ((nextLevelFields != null) && (!nextLevelFields.isEmpty())) {
                    nextLevelFields.forEach(q::offer);
                }
            }
        }

        return r;
    }

    private static Map<String, DstField> flatDstField(DstField srcRoot) {
        Map<String, DstField> r = new HashMap<>();

        // bfs
        Queue<DstField> q = new LinkedList<>();
        q.add(srcRoot);
        r.put(srcRoot.getId(), srcRoot);

        while (!q.isEmpty()) {

            int size = q.size();
            for (int i = 0; i < size; i++) {

                DstField field = q.poll();
                r.put(field.getId(), field);

                List<DstField> nextLevelFields = field.getChildren();
                if ((nextLevelFields != null) && (!nextLevelFields.isEmpty())) {
                    nextLevelFields.forEach(q::offer);
                }
            }
        }

        return r;
    }

    /**
     * 当节点为NULL时返回true
     *
     * @param node
     * @return
     */
    private static boolean isNullNode(JsonNode node) {
        return node == null || node.getNodeType() == JsonNodeType.NULL;
    }

    private static Long parseLongOrThrowEx(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        long r;
        try {
            r = Long.parseLong(value);
            return r;
        } catch (Exception ex) {
            throw new IllegalStateException("parse " + value + " to long failed, name is " + name);
        }
    }

    private static Double parseDoubleOrThrowEx(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        double r;
        try {
            r = Double.parseDouble(value);
            return r;
        } catch (Exception ex) {
            throw new IllegalStateException("parse " + value + " to double failed, name is " + name);
        }
    }
}
