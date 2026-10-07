package com.sloyni.main.config;

public class MessagesConfig {
    private String prefix = "[ISkyblock] ";
    private String prefixColor = "#55FF55";
    private String messageColor = "#FFFF55";
    private String attributeColor = "#AAAAAA";
    private String valueColor = "#55FFFF";
    private String errorColor = "#ff8c8c";

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefixColor() {
        return prefixColor;
    }

    public void setPrefixColor(String prefixColor) {
        this.prefixColor = prefixColor;
    }

    public String getMessageColor() {
        return messageColor;
    }

    public void setMessageColor(String messageColor) {
        this.messageColor = messageColor;
    }

    public String getAttributeColor() {
        return attributeColor;
    }

    public void setAttributeColor(String attributeColor) {
        this.attributeColor = attributeColor;
    }

    public String getValueColor() {
        return valueColor;
    }

    public void setValueColor(String valueColor) {
        this.valueColor = valueColor;
    }

    public String getErrorColor() {
        return errorColor;
    }

    public void setErrorColor(String errorColor) {
        this.errorColor = errorColor;
    }
}
