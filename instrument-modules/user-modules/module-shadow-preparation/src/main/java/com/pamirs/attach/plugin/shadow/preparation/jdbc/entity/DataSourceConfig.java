package com.pamirs.attach.plugin.shadow.preparation.jdbc.entity;

import java.util.List;

public class DataSourceConfig {

    /**
     * 业务库url
     */
    private String url;

    /**
     * 业务库用户名
     */
    private String username;

    /**
     * 数据源类型 0:影子库 1:影子表 2:影子库+影子表
     */
    private int shadowType;

    /**
     * 影子库url
     */
    private String shadowUrl;

    /**
     * 影子库用户名
     */
    private String shadowUsername;

    /**
     * 影子库密码
     */
    private String shadowPassword;

    /**
     * 业务表名称
     */
    private List<String> bizTables;

    /**
     * 字段废弃
     */
    @Deprecated
    private boolean disabled;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getShadowUrl() {
        return shadowUrl;
    }

    public void setShadowUrl(String shadowUrl) {
        this.shadowUrl = shadowUrl;
    }

    public String getShadowUsername() {
        return shadowUsername;
    }

    public void setShadowUsername(String shadowUsername) {
        this.shadowUsername = shadowUsername;
    }

    public String getShadowPassword() {
        return shadowPassword;
    }

    public void setShadowPassword(String shadowPassword) {
        this.shadowPassword = shadowPassword;
    }

    public int getShadowType() {
        return shadowType;
    }

    public void setShadowType(int shadowType) {
        this.shadowType = shadowType;
    }

    public List<String> getBizTables() {
        return bizTables;
    }

    public void setBizTables(List<String> bizTables) {
        this.bizTables = bizTables;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
