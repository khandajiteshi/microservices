package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dao.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){
        System.out.println("orderRequest   "+orderRequest);
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCode = order.getOrderLineItemsList()
                .stream()
                .map(OrderLineItems::getSkuCode).toList();
        //http://localhost:8082/api/inventory?skuCode=iphone_13&skuCode=iphone_13_red
        //call  inventory service, and place order if product is in stock
        System.out.println("   skuCode   "+skuCode);
//        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
//                .uri("http://localhost:8082/api/inventory",
//                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCode).build())
//                .retrieve().bodyToMono(InventoryResponse[].class)
//                .block();
        InventoryResponse[] inventoryResponseArray =  webClientBuilder
                .baseUrl("http://inventory-service/api/inventory") // Replace with the actual URL of the inventory-service
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder.queryParam("skuCode", skuCode).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);
        if (allProductInStock) {
            orderRepository.save(order);
        } else {
           throw new RuntimeException("Product is not in stock, Please try again later");
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }
}
