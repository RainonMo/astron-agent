import React, { useState, useEffect, useCallback } from 'react';
import { Card, Form, Input, Button, message, Alert, Spin, Table, Tag, Badge, Modal, Space } from 'antd';
import { SyncOutlined, SaveOutlined, UserOutlined, PlusOutlined } from '@ant-design/icons';
import {
  getWechatWorkConfig,
  saveWechatWorkConfig,
  syncWechatWorkUsers,
  getWechatWorkSyncUsers,
  addCasdoorUser,
  WechatWorkSyncUser,
} from '@/services/wechat-work';
import styles from './index.module.scss';

interface ConfigForm {
  corpid: string;
  corpsecret: string;
}

const WechatWorkManage: React.FC = () => {
  const [form] = Form.useForm<ConfigForm>();
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [hasConfig, setHasConfig] = useState(false);
  const [initialized, setInitialized] = useState(false);
  const [userList, setUserList] = useState<WechatWorkSyncUser[]>([]);
  const [userListLoading, setUserListLoading] = useState(false);
  const [isAddUserModalOpen, setIsAddUserModalOpen] = useState(false);
  const [addUserForm] = Form.useForm();
  const [addingUser, setAddingUser] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<string>('');

  // 获取配置
  const fetchConfig = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getWechatWorkConfig();
      if (data && data.corpid) {
        form.setFieldsValue({
          corpid: data.corpid,
          corpsecret: '', // 不显示secret
        });
        setHasConfig(true);
      }
    } catch (error: any) {
      message.error(error?.message || '获取配置失败');
    } finally {
      setLoading(false);
      setInitialized(true);
    }
  }, [form]);

  // 获取同步用户列表
  const fetchUserList = useCallback(async () => {
    try {
      setUserListLoading(true);
      const data = await getWechatWorkSyncUsers();
      setUserList(data || []);
    } catch (error: any) {
      message.error(error?.message || '获取用户列表失败');
    } finally {
      setUserListLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfig();
    fetchUserList();
  }, [fetchConfig, fetchUserList]);

  // 保存配置
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await saveWechatWorkConfig(values);
      message.success('保存成功');
      setHasConfig(true);
    } catch (error: any) {
      if (error?.errorFields) {
        // 表单验证错误
        return;
      }
      message.error(error?.message || '保存失败');
    } finally {
      setLoading(false);
    }
  }, [form]);

  // 同步用户
  const handleSync = useCallback(async () => {
    try {
      setSyncing(true);
      const data = await syncWechatWorkUsers();
      message.success(data || '同步成功');
      // 同步完成后刷新用户列表
      await fetchUserList();
    } catch (error: any) {
      message.error(error?.message || '同步失败');
    } finally {
      setSyncing(false);
    }
  }, [fetchUserList]);

  // 打开新增用户弹窗
  const handleOpenAddUserModal = useCallback((userId: string) => {
    setCurrentUserId(userId);
    setIsAddUserModalOpen(true);
    addUserForm.resetFields();
  }, [addUserForm]);

  // 关闭新增用户弹窗
  const handleCloseAddUserModal = useCallback(() => {
    setIsAddUserModalOpen(false);
    setCurrentUserId('');
    addUserForm.resetFields();
  }, [addUserForm]);

  // 提交新增用户
  const handleAddUser = useCallback(async () => {
    try {
      const values = await addUserForm.validateFields();
      if (!currentUserId) {
        message.error('用户ID不能为空');
        return;
      }
      setAddingUser(true);
      await addCasdoorUser(currentUserId, values.phone);
      message.success('用户添加成功');
      setIsAddUserModalOpen(false);
      setCurrentUserId('');
      addUserForm.resetFields();
      // 刷新用户列表以更新按钮状态
      await fetchUserList();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error?.message || '添加用户失败');
    } finally {
      setAddingUser(false);
    }
  }, [addUserForm, currentUserId, fetchUserList]);

  if (!initialized) {
    return (
      <div className={styles.loadingContainer}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className={styles.wechatWorkManage}>
      <div className={styles.header}>
        <h1 className={styles.title}>企业微信管理</h1>
      </div>

      <div className={styles.content}>
        <Card title="企业微信配置" className={styles.configCard}>
          {hasConfig && (
            <Alert
              message="已配置企业微信"
              description="如需修改配置，请重新填写并保存"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}

          <Form
            form={form}
            layout="vertical"
            className={styles.form}
            autoComplete="off"
          >
            <Form.Item
              label="企业ID (CorpID)"
              name="corpid"
              rules={[
                { required: true, message: '请输入企业ID' },
                { max: 128, message: '企业ID不能超过128个字符' },
              ]}
            >
              <Input placeholder="请输入企业微信的CorpID" />
            </Form.Item>

            <Form.Item
              label="应用凭证密钥 (CorpSecret)"
              name="corpsecret"
              rules={[
                { required: true, message: '请输入CorpSecret' },
                { max: 256, message: 'CorpSecret不能超过256个字符' },
              ]}
            >
              <Input.Password
                placeholder="请输入通讯录同步应用的CorpSecret"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                onClick={handleSave}
                loading={loading}
              >
                保存配置
              </Button>
            </Form.Item>
          </Form>
        </Card>

        <Card title="用户同步" className={styles.syncCard}>
          <Alert
            message="同步说明"
            description="点击同步按钮，系统将从企业微信获取成员列表并保存到本地数据库。每个用户只保留第一个部门信息。"
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />

          <Button
            type="primary"
            icon={<SyncOutlined spin={syncing} />}
            onClick={handleSync}
            loading={syncing}
            disabled={!hasConfig}
          >
            {syncing ? '同步中...' : '同步企微用户'}
          </Button>

          {!hasConfig && (
            <div className={styles.tip}>
              请先配置企业微信信息后再进行同步
            </div>
          )}
        </Card>

        <Card
          title={
            <span>
              <UserOutlined style={{ marginRight: 8 }} />
              同步成员列表
              <Badge
                count={userList.length}
                style={{ marginLeft: 8, backgroundColor: '#1890ff' }}
              />
            </span>
          }
          className={styles.userListCard}
        >
          <Table<WechatWorkSyncUser>
            dataSource={userList}
            loading={userListLoading}
            rowKey="id"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
            }}
            columns={[
              {
                title: '用户ID',
                dataIndex: 'userid',
                key: 'userid',
                ellipsis: true,
              },
              {
                title: '部门ID',
                dataIndex: 'department',
                key: 'department',
                width: 100,
              },
              {
                title: '手机号',
                dataIndex: 'phone',
                key: 'phone',
                width: 120,
                render: (phone: string) => phone || '-',
              },
              {
                title: '同步状态',
                dataIndex: 'syncStatus',
                key: 'syncStatus',
                width: 100,
                render: (status: number) => {
                  const statusMap: Record<number, { text: string; color: string }> = {
                    0: { text: '待处理', color: 'default' },
                    1: { text: '成功', color: 'success' },
                    2: { text: '失败', color: 'error' },
                  };
                  const { text, color } = statusMap[status] || { text: '未知', color: 'default' };
                  return <Tag color={color}>{text}</Tag>;
                },
              },
              {
                title: '同步批次',
                dataIndex: 'syncBatchId',
                key: 'syncBatchId',
                ellipsis: true,
              },
              {
                title: '同步时间',
                dataIndex: 'createTime',
                key: 'createTime',
                width: 180,
              },
              {
                title: '操作',
                key: 'action',
                width: 120,
                render: (_: any, record: WechatWorkSyncUser) => (
                  <Button
                    type="primary"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() => handleOpenAddUserModal(record.userid)}
                    disabled={!!record.phone}
                    title={record.phone ? '该用户已添加手机号' : '点击添加用户到Casdoor'}
                  >
                    {record.phone ? '已添加' : '新增用户'}
                  </Button>
                ),
              },
            ]}
          />
        </Card>
      </div>

      {/* 新增用户弹窗 */}
      <Modal
        title={`新增用户 - ${currentUserId}`}
        open={isAddUserModalOpen}
        onOk={handleAddUser}
        onCancel={handleCloseAddUserModal}
        confirmLoading={addingUser}
        okText="确认"
        cancelText="取消"
      >
        <Form
          form={addUserForm}
          layout="vertical"
          autoComplete="off"
          style={{ marginTop: 16 }}
        >
          <Form.Item
            label="手机号"
            name="phone"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
            ]}
          >
            <Input placeholder="请输入用户手机号" maxLength={11} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default WechatWorkManage;
