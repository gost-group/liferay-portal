/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.messageboards.service.permission;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.spring.osgi.OSGiBeanProperties;
import com.liferay.portal.kernel.staging.permission.StagingPermissionUtil;
import com.liferay.portal.kernel.workflow.WorkflowInstance;
import com.liferay.portal.kernel.workflow.permission.WorkflowPermissionUtil;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.BaseModelPermissionChecker;
import com.liferay.portal.security.permission.BaseModelPermissionCheckerUtil;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.ResourceActionsUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.messageboards.model.MBDiscussion;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.service.MBBanLocalServiceUtil;
import com.liferay.portlet.messageboards.service.MBDiscussionLocalServiceUtil;
import com.liferay.portlet.messageboards.service.MBMessageLocalServiceUtil;

import java.util.List;

/**
 * @author Charles May
 * @author Roberto Díaz
 */
@OSGiBeanProperties(
	property = {
		"model.class.name=com.liferay.portlet.messageboards.model.MBDiscussion"
	}
)
public class MBDiscussionPermission implements BaseModelPermissionChecker {

	public static void check(
			PermissionChecker permissionChecker, long companyId, long groupId,
			String className, long classPK, long messageId, long ownerId,
			String actionId)
		throws PortalException {

		if (!contains(
				permissionChecker, companyId, groupId, className, classPK,
				messageId, ownerId, actionId)) {

			throw new PrincipalException();
		}
	}

	public static void check(
			PermissionChecker permissionChecker, long companyId, long groupId,
			String className, long classPK, long ownerId, String actionId)
		throws PortalException {

		if (!contains(
				permissionChecker, companyId, groupId, className, classPK,
				ownerId, actionId)) {

			throw new PrincipalException();
		}
	}

	public static boolean contains(
			PermissionChecker permissionChecker, long companyId, long groupId,
			String className, long classPK, long messageId, long ownerId,
			String actionId)
		throws PortalException {

		MBMessage message = MBMessageLocalServiceUtil.getMessage(messageId);

		if (className.equals(WorkflowInstance.class.getName())) {
			return permissionChecker.hasPermission(
				message.getGroupId(), PortletKeys.WORKFLOW_DEFINITIONS,
				message.getGroupId(), ActionKeys.VIEW);
		}

		if (PropsValues.DISCUSSION_COMMENTS_ALWAYS_EDITABLE_BY_OWNER &&
			(permissionChecker.getUserId() == message.getUserId())) {

			return true;
		}

		if (message.isPending()) {
			Boolean hasPermission = WorkflowPermissionUtil.hasPermission(
				permissionChecker, message.getGroupId(),
				message.getWorkflowClassName(), message.getMessageId(),
				actionId);

			if (hasPermission != null) {
				return hasPermission.booleanValue();
			}
		}

		return contains(
			permissionChecker, companyId, groupId, className, classPK, ownerId,
			actionId);
	}

	public static boolean contains(
		PermissionChecker permissionChecker, long companyId, long groupId,
		String className, long classPK, long ownerId, String actionId) {

		if (MBBanLocalServiceUtil.hasBan(
				groupId, permissionChecker.getUserId())) {

			return false;
		}

		Boolean hasPermission = StagingPermissionUtil.hasPermission(
			permissionChecker, groupId, className, classPK,
			PortletKeys.MESSAGE_BOARDS, actionId);

		if (hasPermission != null) {
			return hasPermission.booleanValue();
		}

		List<String> resourceActions = ResourceActionsUtil.getResourceActions(
			className);

		if (!resourceActions.contains(actionId)) {
			return true;
		}

		if ((ownerId > 0) &&
			permissionChecker.hasOwnerPermission(
				companyId, className, classPK, ownerId, actionId)) {

			return true;
		}

		hasPermission =
			BaseModelPermissionCheckerUtil.containsBaseModelPermission(
				permissionChecker, groupId, className, classPK, actionId);

		if (hasPermission != null) {
			return hasPermission.booleanValue();
		}

		return permissionChecker.hasPermission(
			groupId, className, classPK, actionId);
	}

	@Override
	public void checkBaseModel(
			PermissionChecker permissionChecker, long groupId, long primaryKey,
			String actionId)
		throws PortalException {

		MBDiscussion mbDiscussion =
			MBDiscussionLocalServiceUtil.getMBDiscussion(primaryKey);

		check(
			permissionChecker, mbDiscussion.getCompanyId(), groupId,
			mbDiscussion.getClassName(), mbDiscussion.getClassPK(), primaryKey,
			actionId);
	}

}