package com.hccake.ballcat.system.controller;

import com.hccake.ballcat.common.core.validation.group.CreateGroup;
import com.hccake.ballcat.common.core.validation.group.UpdateGroup;
import com.hccake.ballcat.common.log.operation.annotation.CreateOperationLogging;
import com.hccake.ballcat.common.log.operation.annotation.DeleteOperationLogging;
import com.hccake.ballcat.common.log.operation.annotation.UpdateOperationLogging;
import com.hccake.ballcat.common.model.domain.PageParam;
import com.hccake.ballcat.common.model.domain.PageResult;
import com.hccake.ballcat.common.model.domain.SelectData;
import com.hccake.ballcat.common.model.result.BaseResultCode;
import com.hccake.ballcat.common.model.result.R;
import com.hccake.ballcat.common.model.result.SystemResultCode;
import com.hccake.ballcat.system.component.PasswordHelper;
import com.hccake.ballcat.system.constant.SysUserConst;
import com.hccake.ballcat.system.converter.SysUserConverter;
import com.hccake.ballcat.system.model.dto.SysUserDTO;
import com.hccake.ballcat.system.model.dto.SysUserPassDTO;
import com.hccake.ballcat.system.model.dto.SysUserScope;
import com.hccake.ballcat.system.model.entity.SysRole;
import com.hccake.ballcat.system.model.entity.SysUser;
import com.hccake.ballcat.system.model.qo.SysUserQO;
import com.hccake.ballcat.system.model.vo.SysUserInfo;
import com.hccake.ballcat.system.model.vo.SysUserPageVO;
import com.hccake.ballcat.system.service.SysUserRoleService;
import com.hccake.ballcat.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构
 *
 * @author hccake 2020-09-24 20:16:15
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@Tag(name = "用户管理模块")
public class SysUserController {

	private final SysUserService sysUserService;

	private final SysUserRoleService sysUserRoleService;

	private final PasswordHelper passwordHelper;

	/**
	 * 分页查询用户
	 * @param pageParam 参数集
	 * @return 用户集合
	 */
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('system:user:read')")
	@Operation(summary = "分页查询系统用户")
	public R<PageResult<SysUserPageVO>> getUserPage(@Validated PageParam pageParam, SysUserQO qo) {
		return R.ok(sysUserService.queryPage(pageParam, qo));
	}

	/**
	 * 获取用户Select
	 * @return 用户SelectData
	 */
	@GetMapping("/select")
	@PreAuthorize("@per.hasPermission('system:user:read')")
	@Operation(summary = "获取用户下拉列表数据")
	public R<List<SelectData<Void>>> listSelectData(
			@RequestParam(value = "userTypes", required = false) List<Integer> userTypes) {
		return R.ok(sysUserService.listSelectData(userTypes));
	}

	/**
	 * 获取指定用户的基本信息
	 * @param userId 用户ID
	 * @return SysUserInfo
	 */
	@GetMapping("/{userId}")
	@PreAuthorize("@per.hasPermission('system:user:read')")
	@Operation(summary = "获取指定用户的基本信息")
	public R<SysUserInfo> getSysUserInfo(@PathVariable("userId") Long userId) {
		SysUser sysUser = sysUserService.getById(userId);
		if (sysUser == null) {
			return R.ok();
		}
		SysUserInfo sysUserInfo = SysUserConverter.INSTANCE.poToInfo(sysUser);
		return R.ok(sysUserInfo);
	}

	/**
	 * 新增用户
	 * @param sysUserDTO userInfo
	 * @return success/false
	 */
	@PostMapping
	@CreateOperationLogging(msg = "新增系统用户")
	@PreAuthorize("@per.hasPermission('system:user:add')")
	@Operation(summary = "新增系统用户", description = "新增系统用户")
	public R<Void> addSysUser(@Validated({ Default.class, CreateGroup.class }) @RequestBody SysUserDTO sysUserDTO) {
		SysUser user = sysUserService.getByUsername(sysUserDTO.getUsername());
		if (user != null) {
			return R.failed(BaseResultCode.LOGIC_CHECK_ERROR, "用户名已存在");
		}

		// 明文密码
		String rawPassword = passwordHelper.decodeAes(sysUserDTO.getPass());
		sysUserDTO.setPassword(rawPassword);

		// 密码规则校验
		if (passwordHelper.validateRule(rawPassword)) {
			return sysUserService.addSysUser(sysUserDTO) ? R.ok()
					: R.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增系统用户失败");
		}
		else {
			return R.failed(SystemResultCode.BAD_REQUEST, "密码格式不符合规则!");
		}
	}

	/**
	 * 修改用户个人信息
	 * @param sysUserDto userInfo
	 * @return success/false
	 */
	@PutMapping
	@UpdateOperationLogging(msg = "修改系统用户")
	@PreAuthorize("@per.hasPermission('system:user:edit')")
	@Operation(summary = "修改系统用户", description = "修改系统用户")
	public R<Void> updateUserInfo(@Validated({ Default.class, UpdateGroup.class }) @RequestBody SysUserDTO sysUserDto) {
		return sysUserService.updateSysUser(sysUserDto) ? R.ok()
				: R.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改系统用户失败");
	}

	/**
	 * 删除用户信息
	 */
	@DeleteMapping("/{userId}")
	@DeleteOperationLogging(msg = "通过id删除系统用户")
	@PreAuthorize("@per.hasPermission('system:user:del')")
	@Operation(summary = "通过id删除系统用户", description = "通过id删除系统用户")
	public R<Void> deleteByUserId(@PathVariable("userId") Long userId) {
		return sysUserService.deleteByUserId(userId) ? R.ok()
				: R.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "删除系统用户失败");
	}

	/**
	 * 获取用户 所拥有的角色ID
	 * @param userId userId
	 */
	@GetMapping("/scope/{userId}")
	@PreAuthorize("@per.hasPermission('system:user:grant')")
	public R<SysUserScope> getUserRoleIds(@PathVariable("userId") Long userId) {

		List<SysRole> roleList = sysUserRoleService.listRoles(userId);

		List<String> roleCodes = new ArrayList<>();
		if (!CollectionUtils.isEmpty(roleList)) {
			roleList.forEach(role -> roleCodes.add(role.getCode()));
		}

		SysUserScope sysUserScope = new SysUserScope();
		sysUserScope.setRoleCodes(roleCodes);

		return R.ok(sysUserScope);
	}

	/**
	 * 修改用户权限信息 比如角色 数据权限等
	 * @param sysUserScope sysUserScope
	 * @return success/false
	 */
	@PutMapping("/scope/{userId}")
	@UpdateOperationLogging(msg = "系统用户授权")
	@PreAuthorize("@per.hasPermission('system:user:grant')")
	@Operation(summary = "系统用户授权", description = "系统用户授权")
	public R<Void> updateUserScope(@PathVariable("userId") Long userId, @RequestBody SysUserScope sysUserScope) {
		return sysUserService.updateUserScope(userId, sysUserScope) ? R.ok()
				: R.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "系统用户授权失败");
	}

	/**
	 * 修改用户密码
	 */
	@PutMapping("/pass/{userId}")
	@UpdateOperationLogging(msg = "修改系统用户密码")
	@PreAuthorize("@per.hasPermission('system:user:pass')")
	@Operation(summary = "修改系统用户密码", description = "修改系统用户密码")
	public R<Void> updateUserPass(@PathVariable("userId") Long userId, @RequestBody SysUserPassDTO sysUserPassDTO) {
		String pass = sysUserPassDTO.getPass();
		if (!pass.equals(sysUserPassDTO.getConfirmPass())) {
			return R.failed(SystemResultCode.BAD_REQUEST, "两次密码输入不一致!");
		}

		// 解密明文密码
		String rawPassword = passwordHelper.decodeAes(pass);
		// 密码规则校验
		if (passwordHelper.validateRule(rawPassword)) {
			return sysUserService.updatePassword(userId, rawPassword) ? R.ok()
					: R.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改用户密码失败！");
		}
		else {
			return R.failed(SystemResultCode.BAD_REQUEST, "密码格式不符合规则!");
		}
	}

	/**
	 * 批量修改用户状态
	 */
	@PutMapping("/status")
	@UpdateOperationLogging(msg = "批量修改用户状态")
	@PreAuthorize("@per.hasPermission('system:user:edit')")
	@Operation(summary = "批量修改用户状态", description = "批量修改用户状态")
	public R<Void> updateUserStatus(@NotEmpty(message = "用户ID不能为空") @RequestBody List<Long> userIds,
			@NotNull(message = "用户状态不能为空") @RequestParam("status") Integer status) {

		if (!SysUserConst.Status.NORMAL.getValue().equals(status)
				&& !SysUserConst.Status.LOCKED.getValue().equals(status)) {
			throw new ValidationException("不支持的用户状态！");
		}
		return sysUserService.updateUserStatusBatch(userIds, status) ? R.ok()
				: R.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "批量修改用户状态！");
	}

	@UpdateOperationLogging(msg = "修改系统用户头像")
	@PreAuthorize("@per.hasPermission('system:user:edit')")
	@PostMapping("/avatar")
	@Operation(summary = "修改系统用户头像", description = "修改系统用户头像")
	public R<String> updateAvatar(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) {
		String objectName;
		try {
			objectName = sysUserService.updateAvatar(file, userId);
		}
		catch (IOException e) {
			log.error("修改系统用户头像异常", e);
			return R.failed(BaseResultCode.FILE_UPLOAD_ERROR);
		}
		return R.ok(objectName);
	}

}
