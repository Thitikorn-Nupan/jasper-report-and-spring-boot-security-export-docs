package com.ttknp.apiandjasper.entities;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "order_items")
public class OrderItem {
    @Column("oi_id")
    private Long oiId;
    @Column("image_url")
    private String imageUrl;
    private Integer quantity;
    private Double price;

    public OrderItem() {
    }

    public OrderItem(Long oiId, String imageUrl, Integer quantity, Double price) {
        this.oiId = oiId;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getOiId() {
        return oiId;
    }

    public void setOiId(Long oiId) {
        this.oiId = oiId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

}
