package br.com.microservices.orchestrated.orderservice.core.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "order")
public class Order {

    @Id
    private String id;
    private List<OrderProducts> orderProducts;
    private LocalDateTime createdAt;
    private String transactionId;
    private double totalAmount;
    private int totalItens;

    public String getId() {
        return id;
    }

    public List<OrderProducts> getOrderProducts() {
        return orderProducts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getTotalItens() {
        return totalItens;
    }

    public void setOrderProducts(List<OrderProducts> orderProducts) {
        this.orderProducts = orderProducts;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setTotalItens(int totalItens) {
        this.totalItens = totalItens;
    }
}
