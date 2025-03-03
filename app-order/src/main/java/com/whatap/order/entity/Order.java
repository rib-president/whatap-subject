package com.whatap.order.entity;

import com.whatap.order.enums.OrderStatus;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DynamicInsert
@DynamicUpdate
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order__order")
@Entity
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
  private BigInteger id;

  @Column(name = "status", nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  @Setter
  private OrderStatus status;

  @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
  @Setter
  private BigDecimal totalPrice;

  @Column(name = "orderer_name", nullable = false, length = 16)
  private String ordererName;

  @Column(name = "orderer_phone_number", nullable = false, length = 11)
  private String ordererPhoneNumber;

  @Column(name = "orderer_address", nullable = false, length = 64)
  private String ordererAddress;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
  @Setter
  private List<OrderItem> orderItems;

  public void update(OrderStatus status, BigDecimal totalPrice, String ordererName, String ordererPhoneNumber, String ordererAddress) {
    Optional.ofNullable(status).ifPresent(s -> this.status = s);
    Optional.ofNullable(totalPrice).ifPresent(t -> this.totalPrice = t);
    Optional.ofNullable(ordererName).ifPresent(on -> this.ordererName = on);
    Optional.ofNullable(ordererPhoneNumber).ifPresent(op -> this.ordererPhoneNumber = op);
    Optional.ofNullable(ordererAddress).ifPresent(oa -> this.ordererAddress = oa);
  }
}
