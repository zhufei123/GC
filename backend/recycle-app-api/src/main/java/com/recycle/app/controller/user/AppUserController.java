package com.recycle.app.controller.user;

import com.recycle.app.dto.AddressDTO;
import com.recycle.app.service.AppUserService;
import com.recycle.app.support.CurrentUser;
import com.recycle.app.vo.FavoriteStationVO;
import com.recycle.app.vo.UserMeVO;
import com.recycle.common.core.R;
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

@Tag(name = "App-用户与地址")
@RestController
@RequestMapping("/app-api/user")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService userService;

    @Operation(summary = "当前用户资料")
    @GetMapping("/me")
    public R<UserMeVO> me() {
        return R.ok(userService.me(CurrentUser.id()));
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
