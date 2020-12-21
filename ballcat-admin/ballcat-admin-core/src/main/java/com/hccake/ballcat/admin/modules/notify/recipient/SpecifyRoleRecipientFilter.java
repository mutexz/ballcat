package com.hccake.ballcat.admin.modules.notify.recipient;

import com.hccake.ballcat.admin.constants.NotifyRecipientFilterType;
import com.hccake.ballcat.admin.modules.sys.model.entity.SysUser;
import com.hccake.ballcat.admin.modules.sys.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Hccake 2020/12/21
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class SpecifyRoleRecipientFilter implements RecipientFilter {

	private final SysUserService sysUserService;

	/**
	 * 当前筛选器对应的筛选类型
	 * @return 筛选类型对应的标识
	 * @see NotifyRecipientFilterType
	 */
	@Override
	public Integer filterType() {
		return NotifyRecipientFilterType.SPECIFY_ROLE.getValue();
	}

	/**
	 * 接收者筛选
	 * @param filterCondition 筛选条件
	 * @return 接收者集合
	 */
	@Override
	public List<SysUser> filter(List<Object> filterCondition) {
		List<String> roleCodes = filterCondition.stream().map(x -> (String) x).collect(Collectors.toList());
		return sysUserService.selectUsersByRoleCodes(roleCodes);
	}

}
