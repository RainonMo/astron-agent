import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Button, 
  Modal, 
  Form, 
  Input, 
  Switch, 
  message, 
  Space, 
  Popconfirm,
  Card,
  Row,
  Col,
  Typography
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { 
  getWechatBotPage, 
  createWechatBot, 
  updateWechatBot, 
  deleteWechatBot, 
  generateBotKey 
} from '@/services/wechat-bot';
import type { WechatBotConfig } from '@/types/wechat-bot';

const { Title, Text } = Typography;

const WechatBotManagement: React.FC = () => {
  const { t } = useTranslation();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<WechatBotConfig | null>(null);
  const [data, setData] = useState<WechatBotConfig[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [searchKeyword, setSearchKeyword] = useState('');

  // 获取数据
  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await getWechatBotPage({
        current: pagination.current,
        size: pagination.pageSize,
        keyword: searchKeyword,
      });
      
      setData(response.records);
      setPagination(prev => ({
        ...prev,
        total: response.total,
      }));
    } catch (error) {
      message.error('获取数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [pagination.current, pagination.pageSize, searchKeyword]);

  // 处理表格分页变化
  const handleTableChange = (pager: any) => {
    setPagination({
      current: pager.current,
      pageSize: pager.pageSize,
      total: pagination.total,
    });
  };

  // 打开新增/编辑模态框
  const showModal = (record?: WechatBotConfig) => {
    setEditingRecord(record || null);
    setModalVisible(true);
    
    if (record) {
      form.setFieldsValue(record);
    } else {
      form.resetFields();
      // 生成默认botKey
      generateBotKey().then(key => {
        form.setFieldValue('botKey', key);
      });
    }
  };

  // 关闭模态框
  const closeModal = () => {
    setModalVisible(false);
    setEditingRecord(null);
    form.resetFields();
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      
      if (editingRecord) {
        await updateWechatBot({ ...values, id: editingRecord.id });
        message.success('更新成功');
      } else {
        await createWechatBot(values);
        message.success('创建成功');
      }
      
      closeModal();
      fetchData();
    } catch (error) {
      message.error(editingRecord ? '更新失败' : '创建失败');
    }
  };

  // 删除记录
  const handleDelete = async (id: number) => {
    try {
      await deleteWechatBot(id);
      message.success('删除成功');
      fetchData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 复制回调URL
  const copyCallbackUrl = (url: string) => {
    navigator.clipboard.writeText(url);
    message.success('回调URL已复制到剪贴板');
  };

  // 表格列定义
  const columns = [
    {
      title: '机器人Key',
      dataIndex: 'botKey',
      key: 'botKey',
    },
    {
      title: '企业ID',
      dataIndex: 'corpId',
      key: 'corpId',
    },
    {
      title: '应用ID',
      dataIndex: 'agentId',
      key: 'agentId',
    },
    {
      title: '智能体ID',
      dataIndex: 'agentIdRef',
      key: 'agentIdRef',
    },
    {
      title: '回调URL',
      dataIndex: 'callbackUrl',
      key: 'callbackUrl',
      render: (url: string) => (
        <Space>
          <Text ellipsis style={{ maxWidth: 200 }}>{url}</Text>
          <Button 
            icon={<CopyOutlined />} 
            size="small" 
            onClick={() => copyCallbackUrl(url)}
          />
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (isActive: boolean) => (
        <Switch checked={isActive} disabled />
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: WechatBotConfig) => (
        <Space>
          <Button 
            icon={<EditOutlined />} 
            onClick={() => showModal(record)}
            size="small"
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个机器人配置吗？"
            onConfirm={() => handleDelete(record.id!)}
            okText="确定"
            cancelText="取消"
          >
            <Button icon={<DeleteOutlined />} danger size="small">
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
          <Col>
            <Title level={4}>企业微信机器人管理</Title>
          </Col>
          <Col>
            <Button 
              type="primary" 
              icon={<PlusOutlined />} 
              onClick={() => showModal()}
            >
              新增机器人
            </Button>
          </Col>
        </Row>

        <Table
          dataSource={data}
          columns={columns}
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
          rowKey="id"
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑机器人配置' : '新增机器人配置'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={closeModal}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="botKey"
            label="机器人Key"
            rules={[{ required: true, message: '请输入机器人Key' }]}
          >
            <Input placeholder="自动生成或手动输入" />
          </Form.Item>

          <Form.Item
            name="corpId"
            label="企业ID"
            rules={[{ required: true, message: '请输入企业ID' }]}
          >
            <Input placeholder="请输入企业微信企业ID" />
          </Form.Item>

          <Form.Item
            name="agentId"
            label="应用ID"
            rules={[{ required: true, message: '请输入应用ID' }]}
          >
            <Input placeholder="请输入企业微信应用ID" />
          </Form.Item>

          <Form.Item
            name="token"
            label="Token"
            rules={[{ required: true, message: '请输入Token' }]}
          >
            <Input.Password placeholder="请输入企业微信回调Token" />
          </Form.Item>

          <Form.Item
            name="encodingAesKey"
            label="EncodingAESKey"
            rules={[{ required: true, message: '请输入EncodingAESKey' }]}
          >
            <Input.Password placeholder="请输入企业微信消息加解密密钥" />
          </Form.Item>

          <Form.Item
            name="agentIdRef"
            label="智能体ID"
            rules={[{ required: true, message: '请输入关联的智能体ID' }]}
          >
            <Input placeholder="请输入要对接的智能体ID" />
          </Form.Item>

          <Form.Item
            name="isActive"
            label="启用状态"
            valuePropName="checked"
          >
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default WechatBotManagement;