package com.pamirs.attach.plugin.apache.dubbo.adpater.impl;

import com.pamirs.attach.plugin.apache.dubbo.adpater.AttachmentCleaner;
import org.apache.dubbo.rpc.RpcInvocation;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/15 8:52 PM
 */
public class DubboLowerAttachmentCleaner implements AttachmentCleaner {
    @Override
    public void removeAttachment(String key, RpcInvocation invocation) {
    }
}
