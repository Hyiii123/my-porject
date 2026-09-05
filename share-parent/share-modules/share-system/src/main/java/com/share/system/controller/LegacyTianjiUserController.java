package com.share.system.controller;

import com.github.pagehelper.PageInfo;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.common.security.utils.SecurityUtils;
import com.share.education.api.RemoteTeacherProfileService;
import com.share.system.api.domain.SysRole;
import com.share.system.api.domain.SysUser;
import com.share.system.service.ISysRoleService;
import com.share.system.service.ISysUserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 天机学堂旧用户接口适配层。
 *
 * <p>天机前端沿用了 /us/users、/us/students 等路径，而若依系统服务的
 * 原生路径是 /user。这里仅做协议和字段转换，底层仍复用若依用户、角色服务，
 * 不新增第二套用户表。</p>
 */
@RestController
public class LegacyTianjiUserController extends BaseController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private RemoteTeacherProfileService teacherProfileService;

    @RequiresLogin
    @GetMapping("/users/me")
    public AjaxResult currentUser() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userId == null ? null : userService.selectUserById(userId);
        if (user == null) {
            return error("当前登录用户不存在");
        }
        return success(toView(user));
    }

    @RequiresPermissions("system:user:list")
    @GetMapping({"/users", "/users/"})
    public TableDataInfo users(SysUser query, @RequestParam(required = false) String type) {
        applyLegacyType(query, type);
        startPage();
        List<SysUser> users = userService.selectUserList(query);
        return table(users);
    }

    @RequiresPermissions("system:user:list")
    @GetMapping({"/students/page", "/teachers/page", "/staffs/page"})
    public TableDataInfo roleUsers(SysUser query, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.endsWith("/students/page")) {
            query.setUserType("01");
        } else if (path.endsWith("/teachers/page")) {
            query.setUserType("02");
        } else if (path.endsWith("/staffs/page")) {
            query.setUserType("03");
        }
        // 天机端 1/0 表示启用/禁用，若依字段 0/1 表示正常/停用。
        if ("1".equals(query.getStatus())) {
            query.setStatus("0");
        } else if ("0".equals(query.getStatus())) {
            query.setStatus("1");
        }
        startPage();
        List<SysUser> users = userService.selectUserList(query);
        return table(users);
    }

    @RequiresPermissions("system:user:query")
    @GetMapping("/users/{userId}")
    public AjaxResult user(@PathVariable Long userId) {
        SysUser value = userService.selectUserById(userId);
        return value == null ? error("用户不存在") : success(toView(value));
    }

    @RequiresPermissions("system:user:add")
    @PostMapping("/users")
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        SysUser user = fromBody(body);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword("123456");
        }
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setCreateBy(SecurityUtils.getUsername());
        int rows = userService.insertUser(user);
        if (rows > 0) syncTeacherProfile(user, body);
        return toAjax(rows);
    }

    @RequiresPermissions("system:user:edit")
    @PutMapping({"/users", "/users/{userId}"})
    public AjaxResult edit(@PathVariable(required = false) Long userId,
            @RequestBody Map<String, Object> body) {
        SysUser user = fromBody(body);
        if (userId != null) {
            user.setUserId(userId);
        }
        if (user.getUserId() == null) {
            user.setUserId(SecurityUtils.getUserId());
        }
        if (user.getUserId() == null) {
            return error("用户编号不能为空");
        }
        SysUser existing = userService.selectUserById(user.getUserId());
        if (existing != null && user.getUserType() == null) user.setUserType(existing.getUserType());
        user.setUpdateBy(SecurityUtils.getUsername());
        // /us/users 是旧业务端的兼容接口。只有请求明确携带 roleIds/roleId 时才走
        // 若依的完整 updateUser 流程，避免学员、教师资料编辑时意外清空已有角色。
        int rows = hasRoleAssignment(body)
                ? userService.updateUser(user)
                : userService.updateUserProfile(user);
        if (rows > 0) syncTeacherProfile(user, body);
        return toAjax(rows);
    }

    @RequiresPermissions("system:user:remove")
    @DeleteMapping("/users/{userIds}")
    public AjaxResult remove(@PathVariable String userIds) {
        Long[] ids = Arrays.stream(userIds.split(","))
                .map(this::parseLong)
                .filter(value -> value != null)
                .toArray(Long[]::new);
        if (ids.length == 0 || Arrays.asList(ids).contains(SecurityUtils.getUserId())) {
            return error("用户编号无效或不能删除当前用户");
        }
        List<Long> teacherIds = Arrays.stream(ids)
                .map(userService::selectUserById)
                .filter(user -> user != null && "02".equals(user.getUserType()))
                .map(SysUser::getUserId)
                .toList();
        int rows = userService.deleteUserByIds(ids);
        if (rows > 0) teacherIds.forEach(this::deleteTeacherProfile);
        return toAjax(rows);
    }

    @RequiresPermissions("system:user:edit")
    @PutMapping("/users/{userId}/password/default")
    public AjaxResult resetDefaultPassword(@PathVariable Long userId) {
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            return error("用户不存在");
        }
        return toAjax(userService.resetUserPwd(user.getUserName(), SecurityUtils.encryptPassword("123456")));
    }

    @RequiresPermissions("system:user:edit")
    @PutMapping("/users/{userId}/status/{status}")
    public AjaxResult updateStatus(@PathVariable Long userId, @PathVariable String status) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        // 天机旧接口 1 表示启用、0 表示禁用；若依正好相反。
        user.setStatus("1".equals(status) ? "0" : "1");
        SysUser existing = userService.selectUserById(userId);
        if (existing != null) user.setUserType(existing.getUserType());
        int rows = userService.updateUserStatus(user);
        if (rows > 0) syncTeacherProfile(user, Map.of());
        return toAjax(rows);
    }

    @RequiresLogin
    @PutMapping("/students")
    public AjaxResult updateStudent(@RequestBody Map<String, Object> body) {
        SysUser user = fromBody(body);
        user.setUserId(SecurityUtils.getUserId());
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserProfile(user));
    }

    @RequiresPermissions("system:user:edit")
    @PutMapping("/students/password")
    public AjaxResult updatePassword(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userId == null ? null : userService.selectUserById(userId);
        if (user == null) {
            return error("当前登录用户不存在");
        }
        String password = text(body, "newPassword", text(body, "password", null));
        if (password == null || password.length() < 6) {
            return error("新密码长度不能少于6位");
        }
        return toAjax(userService.resetUserPwd(user.getUserName(), SecurityUtils.encryptPassword(password)));
    }

    @RequiresLogin
    @GetMapping("/users/checkCellphone")
    public AjaxResult checkCellphone(@RequestParam(required = false) String cellPhone,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String phonenumber) {
        SysUser user = new SysUser();
        user.setPhonenumber(firstNonBlank(cellPhone, phone, phonenumber));
        return success(userService.checkPhoneUnique(user));
    }

    @RequiresLogin
    @GetMapping("/users/checkPasswd/{oldPassword}")
    public AjaxResult checkPassword(@PathVariable String oldPassword) {
        Long currentUserId = SecurityUtils.getUserId();
        SysUser currentUser = currentUserId == null ? null : userService.selectUserById(currentUserId);
        if (currentUser == null || currentUser.getPassword() == null) {
            return error("当前登录用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("right", oldPassword != null
                && SecurityUtils.matchesPassword(oldPassword, currentUser.getPassword()));
        return success(result);
    }

    @RequiresPermissions("system:role:list")
    @GetMapping("/roles")
    public AjaxResult roles() {
        return success(roleService.selectRoleAll());
    }

    private TableDataInfo table(List<SysUser> users) {
        List<Map<String, Object>> rows = users == null ? List.of() : users.stream().map(this::toView).toList();
        TableDataInfo result = new TableDataInfo();
        result.setCode(200);
        result.setMsg("查询成功");
        result.setRows(rows);
        result.setTotal(users == null ? 0 : new PageInfo<>(users).getTotal());
        return result;
    }

    private Map<String, Object> toView(SysUser user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getUserId());
        view.put("userId", user.getUserId());
        view.put("username", user.getUserName());
        view.put("userName", user.getUserName());
        view.put("nickname", user.getNickName());
        view.put("nickName", user.getNickName());
        view.put("name", user.getNickName());
        view.put("avatar", user.getAvatar());
        view.put("icon", user.getAvatar());
        view.put("phone", user.getPhonenumber());
        view.put("phonenumber", user.getPhonenumber());
        view.put("email", user.getEmail());
        view.put("sex", user.getSex());
        view.put("gender", parseInt(user.getSex()));
        view.put("status", user.getStatus());
        view.put("enabled", "0".equals(user.getStatus()) ? 1 : 0);
        view.put("createTime", user.getCreateTime());
        view.put("remark", user.getRemark());
        view.put("userType", user.getUserType());
        view.put("type", legacyType(user.getUserType()));
        Map<String, Object> teacher = teacherProfile(user.getUserType(), user.getUserId());
        if (teacher != null) {
            putIfPresent(view, "avatar", teacher.get("avatar"));
            putIfPresent(view, "icon", teacher.get("avatar"));
            putIfPresent(view, "title", teacher.get("title"));
            putIfPresent(view, "job", teacher.get("title"));
            putIfPresent(view, "introduction", teacher.get("introduction"));
            putIfPresent(view, "intro", teacher.get("introduction"));
            putIfPresent(view, "specialty", teacher.get("specialty"));
            view.put("courses", teacher.getOrDefault("courses", 0));
            view.put("courseCount", teacher.getOrDefault("courseCount", 0));
            view.put("students", teacher.getOrDefault("students", 0));
            view.put("studentCount", teacher.getOrDefault("studentCount", 0));
            view.put("rating", teacher.getOrDefault("rating", 0));
        }
        List<SysRole> roles = user.getRoles();
        // selectRolesByUserId 返回的是“全部角色 + flag”，不能直接把全部角色
        // 当作用户已分配角色，否则列表会把每个用户都显示成拥有所有角色，
        // 同时 roleIds 也会因为未完成映射而变成 null。
        boolean completeRoleMapping = roles != null && !roles.isEmpty() && roles.stream()
                .allMatch(role -> role != null && role.getRoleId() != null);
        if (!completeRoleMapping && user.getUserId() != null) {
            List<Long> assignedRoleIds = roleService.selectRoleListByUserId(user.getUserId());
            List<SysRole> allRoles = roleService.selectRoleAll();
            roles = allRoles == null || assignedRoleIds == null ? List.of() : allRoles.stream()
                    .filter(role -> role != null && assignedRoleIds.contains(role.getRoleId()))
                    .toList();
        }
        view.put("roles", roles == null ? List.of() : roles.stream().map(SysRole::getRoleKey).toList());
        view.put("roleIds", roles == null ? List.of() : roles.stream().map(SysRole::getRoleId).toList());
        return view;
    }

    private Map<String, Object> teacherProfile(String userType, Long userId) {
        if (!"02".equals(userType) || userId == null) return null;
        try {
            AjaxResult result = teacherProfileService.getByUserId(userId, SecurityConstants.INNER);
            Object data = result == null ? null : result.get(AjaxResult.DATA_TAG);
            return result != null && result.isSuccess() && data instanceof Map<?, ?> map
                    ? castMap(map) : null;
        } catch (Exception ex) {
            logger.warn("读取教师扩展资料失败，userId={}", userId, ex);
            return null;
        }
    }

    private void syncTeacherProfile(SysUser user, Map<String, Object> body) {
        if (user == null || user.getUserId() == null || !"02".equals(user.getUserType())) return;
        Map<String, Object> source = body == null ? Map.of() : body;
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("teacherName", firstNonBlank(text(source, "nickname", null),
                text(source, "nickName", null), user.getNickName()));
        profile.put("avatarUrl", firstNonBlank(text(source, "avatar", null),
                text(source, "icon", null), text(source, "photo", null), user.getAvatar()));
        profile.put("title", firstNonBlank(text(source, "title", null), text(source, "job", null)));
        profile.put("introduction", firstNonBlank(text(source, "introduction", null),
                text(source, "intro", null)));
        profile.put("specialty", text(source, "specialty", text(source, "expertise", null)));
        profile.put("status", "0".equals(user.getStatus()) || user.getStatus() == null ? 1 : 0);
        profile.put("legacyId", String.valueOf(user.getUserId()));
        try {
            AjaxResult result = teacherProfileService.save(profile, SecurityConstants.INNER);
            if (result == null || !result.isSuccess()) {
                logger.warn("同步教师扩展资料失败，userId={}，msg={}", user.getUserId(),
                        result == null ? "empty response" : result.get(AjaxResult.MSG_TAG));
            }
        } catch (Exception ex) {
            // 教师扩展资料是用户主数据的补充，教育服务短暂不可用时不回滚账号操作。
            logger.warn("同步教师扩展资料异常，userId={}", user.getUserId(), ex);
        }
    }

    private void deleteTeacherProfile(Long userId) {
        try {
            teacherProfileService.deleteByUserId(userId, SecurityConstants.INNER);
        } catch (Exception ex) {
            logger.warn("删除教师扩展资料异常，userId={}", userId, ex);
        }
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !(value instanceof String text && text.isBlank())) target.put(key, value);
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private SysUser fromBody(Map<String, Object> body) {
        Map<String, Object> source = body == null ? Map.of() : body;
        SysUser user = new SysUser();
        user.setUserId(longValue(source.get("userId"), longValue(source.get("id"), null)));
        user.setUserName(text(source, "userName", text(source, "username", null)));
        user.setNickName(text(source, "nickName", text(source, "nickname", text(source, "name", null))));
        user.setAvatar(text(source, "avatar", text(source, "icon", null)));
        user.setPhonenumber(text(source, "phonenumber", text(source, "phone", text(source, "cellPhone", null))));
        user.setEmail(text(source, "email", null));
        user.setSex(text(source, "sex", valueString(source.get("gender"))));
        user.setPassword(text(source, "password", null));
        user.setRemark(text(source, "remark", null));
        // 天机旧接口使用 1=启用、0=禁用；若依 sys_user 使用 0=正常、1=停用。
        String legacyStatus = valueString(source.get("status"));
        user.setStatus("1".equals(legacyStatus) ? "0" : "0".equals(legacyStatus) ? "1" : legacyStatus);
        user.setUserType(legacyUserType(text(source, "type", null)));
        if (source.containsKey("roleIds")) {
            user.setRoleIds(longArray(source.get("roleIds")));
        } else if (source.containsKey("roleId")) {
            user.setRoleIds(longArray(source.get("roleId")));
        }
        return user;
    }

    private boolean hasRoleAssignment(Map<String, Object> body) {
        return body != null && (body.containsKey("roleIds") || body.containsKey("roleId"));
    }

    private Long[] longArray(Object value) {
        if (value == null) return new Long[0];
        if (value instanceof List<?> list) {
            return list.stream().map(item -> longValue(item, null))
                    .filter(item -> item != null).toArray(Long[]::new);
        }
        Long item = longValue(value, null);
        return item == null ? new Long[0] : new Long[]{item};
    }

    private void applyLegacyType(SysUser query, String type) {
        String userType = legacyUserType(type);
        if (userType != null) {
            query.setUserType(userType);
        }
        if ("1".equals(query.getStatus())) {
            query.setStatus("0");
        } else if ("0".equals(query.getStatus())) {
            query.setStatus("1");
        }
    }

    private String legacyUserType(String type) {
        if (type == null || type.isBlank()) return null;
        return switch (type.trim().toLowerCase()) {
            case "admin", "00" -> "00";
            case "student", "01" -> "01";
            case "teacher", "02" -> "02";
            case "employee", "staff", "03" -> "03";
            default -> type.trim();
        };
    }

    private String legacyType(String type) {
        return switch (type == null ? "00" : type) {
            case "01" -> "student";
            case "02" -> "teacher";
            case "03" -> "employee";
            default -> "admin";
        };
    }

    private String text(Map<String, Object> source, String key, String fallback) {
        return text(source.get(key), fallback);
    }

    private String text(Object value, String fallback) {
        String text = value == null ? null : String.valueOf(value).trim();
        return text == null || text.isEmpty() ? fallback : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Long longValue(Object value, Long fallback) {
        try {
            return value == null ? fallback : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Long parseLong(String value) {
        return longValue(value, null);
    }

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String valueString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
