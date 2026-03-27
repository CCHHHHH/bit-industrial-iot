package ${package.Entity};

<#list table.importPackages as pkg>
import ${pkg};
</#list>
<#if swagger>
import io.swagger.v3.oas.annotations.media.Schema;
</#if>
<#if entityLombokModel>
import lombok.Getter;
import lombok.Setter;
<#if chainModel>
import lombok.experimental.Accessors;
</#if>
</#if>

/**
 * <p>
    ${table.comment!}
 * </p>
 *
 * @author ${author}
 * @since ${date}
 */
<#if entityLombokModel>
@Getter
@Setter
<#if chainModel>
@Accessors(chain = true)
</#if>
</#if>
<#if table.convert>
@TableName("${schemaName}${table.name}")
</#if>
<#if swagger>
@Schema(description = "${table.comment!}实体")
</#if>
<#if superEntityClass??>
public class ${entity} extends ${superEntityClass} {
<#else>
public class ${entity} implements Serializable {
</#if>

<#if entitySerialVersionUID>
    private static final long serialVersionUID = 1L;
</#if>
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>
<#if field.keyFlag>
    <#assign keyPropertyName="${field.propertyName}"/>
</#if>

<#if field.comment!?length gt 0>
    /**
     * ${field.comment}
     */
</#if>
<#if field.keyFlag>
<#-- 主键 -->
    @TableId("${field.annotationColumnName}")
<#elseif field.fill??>
<#-- 填充字段 -->
    @TableField(value = "${field.annotationColumnName}", fill = FieldFill.${field.fill})
<#elseif field.logicDelete>
<#-- 逻辑删除字段 -->
    @TableLogic
    @TableField("${field.annotationColumnName}")
<#else>
    @TableField("${field.annotationColumnName}")
</#if>
<#-- 乐观锁注解 -->
<#if (versionFieldName!"") == field.name>
    @Version
</#if>
<#-- 字段注解（自定义） -->
<#if field.comment!?length gt 0>
    @Schema(description = "${field.comment}")
</#if>
    private ${field.propertyType} ${field.propertyName};
</#list>
<#------------  END 字段循环遍历  ---------->

<#if !entityLombokModel>
<#------------  BEGIN getter/setter 循环生成  ---------->
<#list table.fields as field>

    public ${field.propertyType} get${field.capitalName}() {
        return ${field.propertyName};
    }

<#if !field.primitive && field.columnConstantName?? && (field.propertyType.equals("String") || !field.primitive)>
    public void set${field.capitalName}(${field.propertyType} ${field.propertyName}) {
<#else>
    public void set${field.capitalName}(${field.propertyType} ${field.propertyName}) {
</#if>
        this.${field.propertyName} = ${field.propertyName};
    }
</#list>
<#------------  END getter/setter 循环生成  ---------->
</#if>

<#if superEntityClass??>
<#------------  BEGIN toString 方法  ---------->
    @Override
    public String toString() {
        return "${entity}{" +
<#list table.fields as field>
<#if field_index==0>
            "${field.propertyName}=" + ${field.propertyName} +
<#else>
            ", ${field.propertyName}=" + ${field.propertyName} +
</#if>
</#list>
        "}";
    }
<#------------  END toString 方法  ---------->
</#if>
}
