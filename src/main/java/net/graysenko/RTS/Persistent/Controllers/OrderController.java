package net.graysenko.RTS.Persistent.Controllers;

import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.Persistent.DTO.OrderPlacementRequest;
import net.graysenko.RTS.Persistent.DTO.ResponseOrderPlacement;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.OrderEntity;
import net.graysenko.RTS.Persistent.Entities.User;
import net.graysenko.RTS.Persistent.Services.AssetService;
import net.graysenko.RTS.Persistent.Services.OrderService;
import net.graysenko.RTS.Persistent.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final AssetService assetService;

    @PostMapping("/")
    public ResponseEntity<?> testOrderPlacement(@RequestBody OrderPlacementRequest orderRequest) {
        User user = userService.findById(orderRequest.userId());
        Asset asset = assetService.findAssetBySymbol(orderRequest.assetSymbol());

        if (user == null || asset == null) return ResponseEntity.notFound().build();

        if (orderRequest.quantity().signum() <= 0)
            return ResponseEntity.badRequest().build();

        if (orderRequest.orderType() == OrderType.LIMIT) {
            if (orderRequest.price() == null || orderRequest.price().signum() <= 0)
                return ResponseEntity.badRequest().build();
        } else {
            if (orderRequest.price() != null)
                return ResponseEntity.badRequest().build();
        }


        OrderEntity orderEntity = orderService.placeOrder(user, asset,
                orderRequest.side(),
                orderRequest.orderType(),
                orderRequest.price(),
                orderRequest.quantity());



        return new ResponseEntity<>(new ResponseOrderPlacement(
                orderEntity.getId(),
                orderEntity.getStatus(),
                orderEntity.getRemainingQuantity(),
                orderEntity.getQuantity(),
                orderEntity.getPrice()
        ), HttpStatus.OK);
    }
}
