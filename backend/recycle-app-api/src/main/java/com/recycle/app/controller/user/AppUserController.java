package com.recycle.app.controller.user;

import com.recycle.app.dto.AddressDTO;
import com.recycle.app.dto.BindPhoneAlipayDTO;
import com.recycle.app.dto.BindPhoneDTO;
import com.recycle.app.dto.BindPhoneWxDTO;
import com.recycle.app.dto.ProfileDTO;
import com.recycle.app.dto.WithdrawDTO;
import com.recycle.app.service.AppAuthService;
import com.recycle.app.service.AppUserService;
import com.recycle.app.support.CurrentUser;
import com.recycle.app.vo.AppLoginVO;
import com.recycle.app.vo.FavoriteStationVO;
import com.recycle.app.vo.UserMeVO;
import com.recycle.app.vo.UserStatsVO;
import com.recycle.app.vo.WalletVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.member.NotifyLog;
import com.recycle.common.entity.member.UserAddress;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "App-用户与地址")
@RestController
@RequestMapping("/app-api/user")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService userService;
    private final AppAuthService authService;

    @Operation(summary = "当前用户资料")
    @GetMapping("/me")
    public R<UserMeVO> me() {
        return R.ok(userService.me(CurrentUser.id()));
    }

    @Operation(summary = "三方登录后补绑手机号（号码已注册则合并账号并返回新 token）")
    @PostMapping("/bind-phone")
    public R<AppLoginVO> bindPhone(@Valid @RequestBody BindPhoneDTO dto) {
        return R.ok(authService.bindPhone(CurrentUser.id(), dto));
    }

    @Operation(summary = "微信手机号快速验证绑定（getPhoneNumber code）")
    @PostMapping("/bind-phone-wx")
    public R<AppLoginVO> bindPhoneWx(@Valid @RequestBody BindPhoneWxDTO dto) {
        return R.ok(authService.bindPhoneWx(CurrentUser.id(), dto));
    }

    @Operation(summary = "支付宝加密手机号绑定（未配置时请走短信）")
    @PostMapping("/bind-phone-alipay")
    public R<AppLoginVO> bindPhoneAlipay(@RequestBody BindPhoneAlipayDTO dto) {
        return R.ok(authService.bindPhoneAlipay(CurrentUser.id(), dto));
    }

    @Operation(summary = "更新昵称/头像")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        userService.updateProfile(CurrentUser.id(), dto);
        return R.ok();
    }

    @Operation(summary = "我的消息分页（站内通知）")
    @GetMapping("/notices")
    public R<PageResult<NotifyLog>> notices(PageQuery query) {
        return R.ok(userService.notices(CurrentUser.id(), query));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/notices/unread-count")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", userService.unreadNoticeCount(CurrentUser.id())));
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/notices/read-all")
    public R<Void> readAll() {
        userService.readAllNotices(CurrentUser.id());
        return R.ok();
    }

    @Operation(summary = "回收成就统计（完成单数/总重量/总金额/减碳量）")
    @GetMapping("/stats")
    public R<UserStatsVO> stats() {
        return R.ok(userService.stats(CurrentUser.id()));
    }

    @Operation(summary = "我的钱包（余额+最近流水）")
    @GetMapping("/wallet")
    public R<WalletVO> wallet() {
        return R.ok(userService.wallet(CurrentUser.id()));
    }

    @Operation(summary = "钱包提现（mock 立即成功）")
    @PostMapping("/wallet/withdraw")
    public R<Void> withdraw(@Valid @RequestBody WithdrawDTO dto) {
        userService.withdraw(CurrentUser.id(), dto.getAmount());
        return R.ok();
    }

    @Operation(summary = "地址列表")
    @GetMapping("/address/list")
    public R<List<UserAddress>> listAddress() {
        return R.ok(userService.listAddress(CurrentUser.id()));
    }

    @Operation(summary = "新增地址")
    @PostMapping("/address")
    public R<Long> addAddress(@Valid @RequestBody AddressDTO dto) {
        return R.ok(userService.addAddress(CurrentUser.id(), dto));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/address/{id}")
    public R<Void> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        userService.updateAddress(CurrentUser.id(), id, dto);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/address/{id}")
    public R<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(CurrentUser.id(), id);
        return R.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/address/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        userService.setDefault(CurrentUser.id(), id);
        return R.ok();
    }

    @Operation(summary = "是否已收藏该回收站")
    @GetMapping("/favorite/station/{id}")
    public R<Boolean> isFavorite(@PathVariable Long id) {
        return R.ok(userService.isFavorite(CurrentUser.id(), id));
    }

    @Operation(summary = "收藏回收站（幂等）")
    @PostMapping("/favorite/station/{id}")
    public R<Void> addFavorite(@PathVariable Long id) {
        userService.addFavorite(CurrentUser.id(), id);
        return R.ok();
    }

    @Operation(summary = "取消收藏回收站")
    @DeleteMapping("/favorite/station/{id}")
    public R<Void> removeFavorite(@PathVariable Long id) {
        userService.removeFavorite(CurrentUser.id(), id);
        return R.ok();
    }

    @Operation(summary = "我的收藏回收站列表")
    @GetMapping("/favorite/stations")
    public R<List<FavoriteStationVO>> listFavorites() {
        return R.ok(userService.listFavorites(CurrentUser.id()));
    }
}
