package com.campus.trade.order.controller;

import com.campus.trade.common.response.PageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.order.dto.CancelOrderRequest;
import com.campus.trade.order.dto.CreateOrderRequest;
import com.campus.trade.order.service.OrderService;
import com.campus.trade.order.vo.OrderCreatedVO;
import com.campus.trade.order.vo.OrderDetailVO;
import com.campus.trade.order.vo.OrderListItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的订单接口。
 *
 * <p>所有路径都不属于 PublicRequestMatcher，因此必须先通过 LoginInterceptor；
 * 买家、卖家身份均由 UserContext 推导，不接收前端提交的 userId。</p>
 */
@Validated
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 买家创建订单，同时占用商品库存。 */
    @PostMapping
    public Result<OrderCreatedVO> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(request));
    }

    /** 查询当前用户买到或卖出的订单。 */
    @GetMapping
    public Result<PageResult<OrderListItemVO>> listMine(
            @RequestParam(defaultValue = "buyer") @Pattern(regexp = "buyer|seller", message = "订单角色只能是 buyer 或 seller") String role,
            @RequestParam(required = false) @Min(value = 0, message = "订单状态不正确") @Max(value = 5, message = "订单状态不正确") Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数至少为 1") @Max(value = 50, message = "每页条数不能超过 50") int pageSize
    ) {
        return Result.ok(orderService.listMine(role, status, page, pageSize));
    }

    /** 查询订单详情；只有该订单买家或卖家可以读取。 */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getDetail(@PathVariable @Min(value = 1, message = "订单 ID 不正确") Long id) {
        return Result.ok(orderService.getDetail(id));
    }

    /** 卖家确认待确认订单。 */
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable @Min(value = 1, message = "订单 ID 不正确") Long id) {
        orderService.confirm(id);
        return Result.ok();
    }

    /** 买家或卖家取消待确认、已确认订单，并归还商品库存。 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(
            @PathVariable @Min(value = 1, message = "订单 ID 不正确") Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request
    ) {
        orderService.cancel(id, request == null ? null : request.reason());
        return Result.ok();
    }

    /** 买家确认线下交易完成。 */
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable @Min(value = 1, message = "订单 ID 不正确") Long id) {
        orderService.complete(id);
        return Result.ok();
    }
}
