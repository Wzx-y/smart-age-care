import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  BedDouble,
  Bell,
  Building2,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronRight,
  ClipboardCheck,
  ClipboardList,
  Clock3,
  Download,
  FilePlus2,
  Eye,
  HeartPulse,
  LockKeyhole,
  LogOut,
  Mail,
  Menu,
  MoreHorizontal,
  Pencil,
  Plus,
  Phone,
  Search,
  Settings2,
  ShieldCheck,
  ShieldAlert,
  Sparkles,
  UserRound,
  UserCog,
  UsersRound,
  Wifi,
  X,
  Trash2,
} from "lucide-react";
import { createAuthApi } from "./features/auth/auth-api.js";
import { createAdmissionApi } from "./features/admissions/admission-api.js";
import { createCareApi } from "./features/care/care-api.js";
import { GatewayCareWorkspace } from "./features/care/GatewayCareWorkspace.jsx";
import { createExportApi } from "./features/exports/export-api.js";
import { createMasterDataApi } from "./features/master-data/master-data-api.js";
import { createNotificationApi } from "./features/notifications/notification-api.js";
import { GatewayMasterDataWorkspace } from "./features/master-data/GatewayMasterDataWorkspace.jsx";
import { GatewayOperationalOverview } from "./features/operations/GatewayOperationalOverview.jsx";
import { createResidentApi } from "./features/residents/resident-api.js";
import { createSystemApi } from "./features/system/system-api.js";
import { GatewayResidentWorkspace } from "./features/residents/GatewayResidentWorkspace.jsx";
import { AuditWorkspace } from "./features/audit/AuditWorkspace.jsx";
import { TenantCrudPanel } from "./features/system/TenantCrudPanel.jsx";
import { createSessionStore } from "./features/auth/session-store.js";
import { runtimeConfig } from "./lib/runtime-config.js";

const NAV_ITEMS = [
  { id: "overview", label: "机构总览" },
  { id: "residents", label: "长者档案" },
  { id: "admission", label: "入住与床位" },
  { id: "care", label: "照护计划" },
  { id: "master-data", label: "基础资料" },
  { id: "devices", label: "设备中心" },
  { id: "reports", label: "运营报表" },
];

const TENANTS = [
  { id: "yihe", name: "颐和苑养老中心", code: "YH-CARE-001", plan: "专业版", members: 42, residents: 186, region: "上海 · 徐汇" },
  { id: "changle", name: "长乐康养社区", code: "CL-CARE-018", plan: "标准版", members: 19, residents: 74, region: "杭州 · 拱墅" },
  { id: "nanshan", name: "南山颐养院", code: "NS-CARE-024", plan: "专业版", members: 31, residents: 128, region: "苏州 · 姑苏" },
];

const DEFAULT_MEMBERS = [
  { id: 1, name: "张院长", role: "机构管理员", department: "院务办公室", scope: "全机构", status: "已启用", email: "zhang@yiheyuan.care" },
  { id: 2, name: "李雪", role: "护理主管", department: "护理部", scope: "护理单元 A / B", status: "已启用", email: "lixue@yiheyuan.care" },
  { id: 3, name: "何芳", role: "责任护士", department: "护理部", scope: "A 区", status: "已启用", email: "hefang@yiheyuan.care" },
  { id: 4, name: "陈静", role: "社工", department: "服务部", scope: "家属沟通", status: "待激活", email: "chenjing@yiheyuan.care" },
];

const INITIAL_TASKS = [
  { id: 1, time: "09:30", name: "陈玉兰", room: "A-312", task: "压疮风险复评", owner: "李雪", status: "待处理", level: "high" },
  { id: 2, time: "10:00", name: "王素兰", room: "B-208", task: "康复训练陪同", owner: "周敏", status: "进行中", level: "normal" },
  { id: 3, time: "10:30", name: "刘建国", room: "A-106", task: "血压监测与记录", owner: "何芳", status: "待处理", level: "normal" },
  { id: 4, time: "11:00", name: "孙桂英", room: "C-403", task: "家属视频沟通", owner: "陈静", status: "待确认", level: "low" },
];

const RESIDENTS = [
  { name: "王素兰", room: "B-208", tag: "重点关注", tone: "rose", image: "https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=160&q=80" },
  { name: "刘建国", room: "A-106", tag: "复诊提醒", tone: "amber", image: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80" },
  { name: "周淑芬", room: "C-201", tag: "状态稳定", tone: "mint", image: "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&w=160&q=80" },
];

const INITIAL_MODULE_RECORDS = {
  residents: [
    { id: 101, name: "王素兰", room: "B-208", age: "82", contact: "女儿 王敏", status: "重点关注" },
    { id: 102, name: "刘建国", room: "A-106", age: "76", contact: "儿子 刘浩", status: "状态稳定" },
    { id: 103, name: "周淑芬", room: "C-201", age: "79", contact: "女儿 周宁", status: "复诊提醒" },
    { id: 104, name: "孙桂英", room: "C-403", age: "85", contact: "侄女 孙梅", status: "状态稳定" },
  ],
  admission: [
    { id: 201, name: "许月琴", room: "A-205", bed: "A-205-02", date: "2026-07-28", status: "待评估" },
    { id: 202, name: "马振华", room: "B-306", bed: "B-306-01", date: "2026-07-25", status: "已入住" },
    { id: 203, name: "罗秀英", room: "C-108", bed: "C-108-02", date: "2026-07-21", status: "已入住" },
  ],
  care: [
    { id: 301, name: "王素兰", plan: "跌倒风险干预", frequency: "每日 3 次巡视", owner: "李雪", status: "执行中" },
    { id: 302, name: "刘建国", plan: "血压监测计划", frequency: "每日 2 次", owner: "何芳", status: "执行中" },
    { id: 303, name: "周淑芬", plan: "术后康复训练", frequency: "每周 5 次", owner: "周敏", status: "待确认" },
  ],
  devices: [
    { id: 401, name: "紧急呼叫按钮", location: "C-403 卫生间", code: "SOS-C403-01", status: "低电量" },
    { id: 402, name: "睡眠监测带", location: "B-208 房", code: "SLP-B208-02", status: "在线" },
    { id: 403, name: "毫米波雷达", location: "A-106 房", code: "RAD-A106-01", status: "在线" },
    { id: 404, name: "电子围栏", location: "康复花园", code: "FEN-GDN-03", status: "待检修" },
  ],
  reports: [
    { id: 501, name: "7 月机构运营简报", period: "2026 年 7 月", owner: "张院长", date: "2026-07-29", status: "已生成" },
    { id: 502, name: "第二季度照护质量报告", period: "2026 年 Q2", owner: "护理部", date: "2026-07-12", status: "已发布" },
    { id: 503, name: "设备月度巡检报告", period: "2026 年 7 月", owner: "工程组", date: "2026-07-28", status: "待确认" },
  ],
};

const MODULE_CONFIG = {
  residents: { title: "长者档案", subtitle: "集中管理在院长者信息、紧急联系人与照护关注状态", create: "新增长者", fields: [["name", "长者姓名"], ["age", "年龄"], ["room", "房间"], ["contact", "紧急联系人"], ["status", "状态"]], statuses: ["全部", "重点关注", "状态稳定", "复诊提醒"] },
  admission: { title: "入住与床位", subtitle: "安排入住评估，查看房间与床位使用情况", create: "登记入住", fields: [["name", "长者姓名"], ["room", "房间"], ["bed", "床位"], ["date", "入住日期"], ["status", "入住状态"]], statuses: ["全部", "待评估", "已入住"] },
  care: { title: "照护计划", subtitle: "维护个性化照护方案与当前执行责任人", create: "新建计划", fields: [["name", "服务对象"], ["plan", "计划名称"], ["frequency", "服务频次"], ["owner", "负责人"], ["status", "执行状态"]], statuses: ["全部", "执行中", "待确认"] },
  devices: { title: "设备中心", subtitle: "监测房间与公共区域设备运行状态和维护事项", create: "登记设备", fields: [["name", "设备名称"], ["location", "安装位置"], ["code", "设备编号"], ["status", "运行状态"]], statuses: ["全部", "在线", "低电量", "待检修"] },
  reports: { title: "运营报表", subtitle: "汇集机构运营、照护质量与设备巡检的周期性报告", create: "生成报表", fields: [["name", "报表名称"], ["period", "统计周期"], ["owner", "生成部门"], ["date", "生成日期"], ["status", "报表状态"]], statuses: ["全部", "已生成", "已发布", "待确认"] },
};

const EXPORT_STATUS_LABELS = {
  QUEUED: "等待生成",
  GENERATING: "生成中",
  READY: "可下载",
  FAILED: "生成失败",
  EXPIRED: "已过期",
};

const DEMO_OPERATIONAL_REPORT = {
  residentsInResidence: 186,
  enabledBeds: 220,
  occupiedBeds: 186,
  occupancyRate: 84.5,
  admissionApplications: 12,
  discharges: 4,
  scheduledCareTasks: 328,
  completedCareTasks: 307,
  exceptionCareTasks: 5,
  taskCompletionRate: 93.6,
  serviceRecords: 307,
  openFollowUps: 3,
};

const DEMO_EXPORT_JOBS = [
  { id: "demo-export-1", exportType: "OPERATIONAL_SUMMARY_CSV", periodStart: "2026-07-01", periodEnd: "2026-07-31", status: "READY", fileName: "机构运营汇总-2026-07.csv", createdAt: "2026-07-29T09:32:00+08:00", completedAt: "2026-07-29T09:33:00+08:00", expiresAt: "2026-07-30T09:33:00+08:00" },
  { id: "demo-export-2", exportType: "OPERATIONAL_SUMMARY_CSV", periodStart: "2026-06-01", periodEnd: "2026-06-30", status: "EXPIRED", fileName: "机构运营汇总-2026-06.csv", createdAt: "2026-07-01T09:20:00+08:00", completedAt: "2026-07-01T09:21:00+08:00", expiresAt: "2026-07-02T09:21:00+08:00" },
];

function StatusPill({ status }) {
  const className = status === "进行中" ? "pill progress" : status === "已完成" ? "pill done" : status === "待确认" ? "pill review" : "pill pending";
  return <span className={className}>{status}</span>;
}

function MetricCard({ icon: Icon, label, value, note, trend, accent }) {
  return (
    <article className="metric-card">
      <div className={`metric-icon ${accent}`}><Icon size={19} strokeWidth={2.1} /></div>
      <div className="metric-meta">
        <span>{label}</span>
        <strong>{value}</strong>
        <small className={trend === "up" ? "positive" : "neutral"}>{note}</small>
      </div>
      <ArrowRight size={17} className="metric-arrow" />
    </article>
  );
}

function Resident360({ resident, tasks, plans, onBack, onOpenCare, onCompleteTask }) {
  const [tab, setTab] = useState("概览");
  const residentTasks = tasks.filter((task) => task.name === resident.name);
  const residentPlans = plans.filter((plan) => plan.name === resident.name);
  return <section className="resident-360"><button type="button" className="back-link" onClick={onBack}><ArrowLeft size={17} />返回长者档案</button><div className="resident-hero panel"><div className="resident-profile"><img src="https://images.unsplash.com/photo-1488161628813-04466f872be2?auto=format&fit=crop&w=300&q=85" alt={resident.name} /><div><p>在院长者 · {resident.room}</p><h1>{resident.name}<span>{resident.age} 岁</span></h1><div className="profile-tags"><span className="tag-risk">跌倒风险 II 级</span><span className="tag-mint">护理计划执行中</span><span className="tag-neutral">入住第 126 天</span></div></div></div><div className="resident-hero-actions"><button type="button" className="button secondary" onClick={onOpenCare}><ClipboardList size={17} />查看护理执行</button><button type="button" className="button primary" onClick={onOpenCare}><Plus size={17} />创建今日任务</button></div></div><div className="resident-tabs" role="tablist">{["概览", "健康评估", "护理计划", "服务记录"].map((item) => <button type="button" key={item} className={tab === item ? "active" : ""} onClick={() => setTab(item)}>{item}</button>)}</div>{tab === "概览" && <div className="resident-detail-grid"><div className="detail-main"><section className="panel detail-panel"><div className="panel-heading"><div><h2>今日照护安排</h2><p>执行完成后自动进入班次交接</p></div><span className="detail-count">{residentTasks.length} 项任务</span></div><div className="resident-task-list">{residentTasks.length ? residentTasks.map((task) => <div className="resident-task-item" key={task.id}><span><b>{task.time}</b><small>{task.task}</small></span><span className="owner"><i>{task.owner.slice(0, 1)}</i>{task.owner}</span><StatusPill status={task.status} />{task.status !== "已完成" ? <button type="button" className="finish-button" aria-label={`完成${task.task}`} onClick={() => onCompleteTask(task.id)}><Check size={16} /></button> : <span className="finished"><Check size={15} /></span>}</div>) : <div className="empty-inline">今日暂未生成任务</div>}</div></section><section className="panel detail-panel"><div className="panel-heading"><div><h2>风险与观察</h2><p>来自最近评估、任务执行和照护记录</p></div><ShieldAlert size={19} className="warning-icon" /></div><div className="risk-grid"><article><span className="risk-dot high" /><div><b>跌倒风险</b><p>昨夜起夜 2 次，夜间巡视尚有 1 次待完成。</p></div></article><article><span className="risk-dot medium" /><div><b>营养观察</b><p>连续两日午餐摄入低于个人基线，建议补充评估。</p></div></article><article><span className="risk-dot low" /><div><b>家属沟通</b><p>女儿已预约今日 15:00 视频探视。</p></div></article></div></section></div><aside className="detail-side"><section className="panel detail-panel"><div className="panel-heading compact"><div><h2>关键评估</h2><p>最近更新：2026-07-28</p></div></div><div className="assessment-list"><div><span>ADL 日常生活能力</span><b>60 <small>轻度依赖</small></b></div><div><span>认知筛查</span><b>21 <small>轻度下降</small></b></div><div><span>营养风险</span><b>中风险 <small>需观察</small></b></div></div></section><section className="panel detail-panel"><div className="panel-heading compact"><div><h2>生效护理计划</h2><p>计划、任务、记录保持关联</p></div></div><div className="plan-mini-list">{residentPlans.length ? residentPlans.map((plan) => <div key={plan.id}><span><HeartPulse size={15} /></span><p><b>{plan.plan}</b><small>{plan.frequency} · {plan.owner}</small></p><ChevronRight size={16} /></div>) : <p className="empty-inline">暂无生效计划</p>}</div></section></aside></div>}{tab === "健康评估" && <section className="panel tab-placeholder"><Activity size={26} /><h2>健康评估</h2><p>下一轮将接入 ADL、认知、营养、压疮与跌倒评估表单，并生成可追溯的版本记录。</p></section>}{tab === "护理计划" && <section className="panel tab-placeholder"><ClipboardCheck size={26} /><h2>护理计划</h2><p>当前长者共有 {residentPlans.length} 个生效计划，可在护理执行中心查看任务完成与异常情况。</p><button type="button" className="button primary" onClick={onOpenCare}>进入护理执行</button></section>}{tab === "服务记录" && <section className="panel tab-placeholder"><FilePlus2 size={26} /><h2>服务记录</h2><p>服务记录将按日期、执行人和计划自动归集，支持交接与质量追溯。</p></section>}</section>;
}

function CareExecutionWorkspace({ tasks, plans, onCompleteTask, onCreate, onEdit, onView }) {
  const [taskFilter, setTaskFilter] = useState("全部");
  const shownTasks = taskFilter === "全部" ? tasks : tasks.filter((task) => task.status === taskFilter);
  return <section className="care-workspace"><div className="module-hero"><div><p className="eyebrow"><span />服务执行闭环</p><h1>护理执行中心</h1><p>从生效护理计划生成当日任务，执行后自动沉淀为服务记录与交接依据。</p></div><button type="button" className="button primary" onClick={() => onCreate("care")}><Plus size={17} />新建护理计划</button></div><section className="care-kpis"><article><span>今日待执行</span><strong>{tasks.filter((task) => task.status === "待处理").length + 4}</strong><small>含 1 项风险巡视</small></article><article><span>执行中</span><strong>{tasks.filter((task) => task.status === "进行中").length}</strong><small>责任人已确认</small></article><article><span>待确认交接</span><strong>{tasks.filter((task) => task.status === "待确认").length}</strong><small>需下一班签收</small></article></section><div className="care-execution-grid"><section className="panel execution-panel"><div className="panel-heading"><div><h2>今日执行队列</h2><p>按计划时间排序，异常与逾期任务优先展示</p></div><button type="button" className="text-action">查看日历<ChevronRight size={16} /></button></div><div className="filter-row">{["全部", "待处理", "进行中", "待确认"].map((item) => <button type="button" key={item} className={taskFilter === item ? "filter active" : "filter"} onClick={() => setTaskFilter(item)}>{item}</button>)}</div><div className="execution-list">{shownTasks.map((task) => <article key={task.id}><div className="execution-time"><i className={task.level} />{task.time}</div><div className="execution-task"><b>{task.task}</b><span>{task.name} · {task.room}</span></div><div className="execution-owner"><span>{task.owner.slice(0, 1)}</span>{task.owner}</div><StatusPill status={task.status} />{task.status !== "已完成" ? <button type="button" className="finish-button" aria-label={`完成${task.task}`} onClick={() => onCompleteTask(task.id)}><Check size={16} /></button> : <span className="finished"><Check size={15} /></span>}</article>)}</div></section><aside className="panel plan-panel"><div className="panel-heading"><div><h2>生效护理计划</h2><p>计划状态影响每日任务生成</p></div><span className="detail-count">{plans.length} 个</span></div><div className="care-plan-list">{plans.map((plan) => <article key={plan.id}><span className="plan-icon"><HeartPulse size={16} /></span><div><b>{plan.plan}</b><small>{plan.name} · {plan.frequency}</small><span>{plan.owner} · {plan.status}</span></div><button type="button" aria-label={`查看${plan.plan}`} onClick={() => onView("care", plan)}><ChevronRight size={17} /></button></article>)}</div><button type="button" className="full-text-button" onClick={() => onCreate("care")}>创建护理计划 <ArrowRight size={16} /></button></aside></div></section>;
}

const CARE_TASK_STATUS_LABELS = {
  PENDING: "待处理",
  IN_PROGRESS: "进行中",
  PENDING_CONFIRMATION: "待确认",
  COMPLETED: "已完成",
  EXCEPTION: "异常",
  OVERDUE: "已逾期",
  CANCELLED: "已取消",
};

const CARE_PLAN_STATUS_LABELS = {
  DRAFT: "草稿",
  PUBLISHED: "执行中",
  SUPERSEDED: "已失效",
  CANCELLED: "已取消",
};

function toGatewayCareTask(source) {
  const scheduledAt = source.scheduledAt ? new Date(source.scheduledAt) : null;
  return {
    id: source.id,
    time: scheduledAt && !Number.isNaN(scheduledAt.valueOf()) ? scheduledAt.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false }) : "--:--",
    name: `长者 #${source.residentId}`,
    room: "当前机构",
    task: source.taskName,
    owner: `成员 #${source.assigneeId}`,
    status: CARE_TASK_STATUS_LABELS[source.status] || source.status,
    level: source.status === "EXCEPTION" || source.status === "OVERDUE" ? "high" : "normal",
    taskVersion: source.version,
    statusCode: source.status,
    planId: source.planId,
    residentId: source.residentId,
    assigneeId: source.assigneeId,
    scheduledAt: source.scheduledAt,
  };
}

function toGatewayCarePlan(source) {
  return {
    id: source.id,
    name: `长者 #${source.residentId}`,
    plan: source.planName,
    frequency: source.frequencyText,
    owner: `成员 #${source.ownerId}`,
    status: CARE_PLAN_STATUS_LABELS[source.status] || source.status,
    planVersion: source.version,
    statusCode: source.status,
    residentId: source.residentId,
  };
}

function GatewayCareExecutionWorkspace({ careApi, residentApi, systemApi, onAnnounce }) {
  const [tasks, setTasks] = useState([]);
  const [plans, setPlans] = useState([]);
  const [followUps, setFollowUps] = useState([]);
  const [handovers, setHandovers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeForm, setActiveForm] = useState(null);
  const [working, setWorking] = useState(false);
  const [selectedPlanId, setSelectedPlanId] = useState("");
  const [templates, setTemplates] = useState([]);
  const [residents, setResidents] = useState([]);
  const [members, setMembers] = useState([]);
  const [serviceRecords, setServiceRecords] = useState([]);
  const [serviceFilters, setServiceFilters] = useState({ residentId: "", serviceDate: "", executorId: "" });
  const [planForm, setPlanForm] = useState({ residentId: "", planName: "", frequencyText: "每日", startDate: new Date().toISOString().slice(0, 10), endDate: "" });
  const [taskForm, setTaskForm] = useState({ planId: "", taskName: "", scheduledAt: "", assigneeId: "" });
  const [templateForm, setTemplateForm] = useState({ planId: "", taskName: "", scheduledTime: "08:00", assigneeId: "" });
  const [generationDate, setGenerationDate] = useState(new Date().toISOString().slice(0, 10));
  const [handoverForm, setHandoverForm] = useState({ shiftDate: new Date().toISOString().slice(0, 10), shiftCode: "DAY", toUserId: "", note: "", taskIds: [] });

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [nextPlans, nextTasks, nextFollowUps, nextHandovers, nextResidents, nextMembers, nextServiceRecords] = await Promise.all([
        careApi.listPlans(), careApi.listTasks(), careApi.listTaskFollowUps(), careApi.listShiftHandovers(),
        residentApi.listResidents(), systemApi.listMembers(), careApi.listServiceRecords(),
      ]);
      setPlans(nextPlans.map(toGatewayCarePlan));
      setTasks(nextTasks.map(toGatewayCareTask));
      setFollowUps(nextFollowUps);
      setHandovers(nextHandovers);
      setResidents(nextResidents);
      setMembers(nextMembers);
      setServiceRecords(nextServiceRecords);
    } catch (requestError) {
      setError(requestError.message || "护理数据加载未完成");
    } finally {
      setLoading(false);
    }
  }, [careApi, residentApi, systemApi]);

  useEffect(() => { refresh(); }, [refresh]);

  const runAction = async (action, successMessage) => {
    try {
      setWorking(true);
      await action();
      await refresh();
      setActiveForm(null);
      onAnnounce(successMessage);
    } catch (requestError) {
      await refresh();
      onAnnounce(requestError.status === 409 ? "记录状态已变化，已刷新最新数据" : (requestError.message || "操作未提交"));
    } finally {
      setWorking(false);
    }
  };

  const loadTemplates = async (planId) => {
    setSelectedPlanId(String(planId));
    try {
      setTemplates(await careApi.listTaskTemplates(planId));
    } catch (requestError) {
      onAnnounce(requestError.message || "任务模板读取未完成");
    }
  };

  const loadServiceRecords = async (nextFilters = serviceFilters) => {
    try {
      setServiceRecords(await careApi.listServiceRecords({
        residentId: nextFilters.residentId || undefined,
        serviceDate: nextFilters.serviceDate || undefined,
        executorId: nextFilters.executorId || undefined,
      }));
    } catch (requestError) {
      onAnnounce(requestError.message || "服务记录读取未完成");
    }
  };

  const taskForForm = activeForm?.type === "complete" || activeForm?.type === "exception"
    ? tasks.find((item) => item.id === activeForm.taskId) : null;
  useEffect(() => {
    if (activeForm?.type === "edit-plan") {
      const plan = activeForm.plan;
      setPlanForm({ residentId: String(plan.residentId), planName: plan.plan, frequencyText: plan.frequency, startDate: plan.startDate || "", endDate: plan.endDate || "" });
    }
  }, [activeForm]);
  const eligibleHandoverTasks = tasks.filter((task) => task.statusCode === "COMPLETED" || task.statusCode === "EXCEPTION");

  if (loading) return <section className="care-workspace"><div className="empty-inline">正在加载当前机构的护理计划与任务...</div></section>;
  if (error) return <section className="care-workspace"><div className="empty-inline">{error}</div><button type="button" className="button secondary" onClick={refresh}>重新加载</button></section>;
  return <section className="care-workspace">
    <div className="module-hero"><div><p className="eyebrow"><span />服务执行闭环</p><h1>护理执行中心</h1><p>计划、模板、每日任务、服务记录、异常跟进与班次交接均在当前机构范围内联动。</p></div><div className="module-hero-actions"><button type="button" className="button secondary" onClick={() => setActiveForm({ type: "generate" })}>生成当日任务</button><button type="button" className="button primary" onClick={() => setActiveForm({ type: "plan" })}><Plus size={17} />新建护理计划</button></div></div>
    {activeForm?.type === "plan" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); void runAction(() => careApi.createPlan({ ...planForm, residentId: Number(planForm.residentId), endDate: planForm.endDate || undefined }), "护理计划草稿已创建"); }}><div className="panel-heading compact"><div><h2>新建护理计划</h2><p>计划需发布后才可生成任务</p></div></div><div className="record-fields"><label>服务长者<select required value={planForm.residentId} onChange={(event) => setPlanForm((value) => ({ ...value, residentId: event.target.value }))}><option value="">请选择当前机构长者</option>{residents.map((resident) => <option key={resident.id} value={resident.id}>{resident.name} · {resident.status}</option>)}</select></label><label>计划名称<input required value={planForm.planName} onChange={(event) => setPlanForm((value) => ({ ...value, planName: event.target.value }))} /></label><label>服务频次<input required value={planForm.frequencyText} onChange={(event) => setPlanForm((value) => ({ ...value, frequencyText: event.target.value }))} /></label><label>开始日期<input required type="date" value={planForm.startDate} onChange={(event) => setPlanForm((value) => ({ ...value, startDate: event.target.value }))} /></label><label>结束日期<input type="date" value={planForm.endDate} onChange={(event) => setPlanForm((value) => ({ ...value, endDate: event.target.value }))} /></label></div><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "保存中…" : "保存草稿"}</button></div></form>}
    {activeForm?.type === "generate" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); void runAction(() => careApi.generateTasks(generationDate), "当日任务已生成，重复生成不会重复写入"); }}><div className="panel-heading compact"><div><h2>生成每日任务</h2><p>仅从生效计划的启用模板生成</p></div></div><label>服务日期<input required type="date" value={generationDate} onChange={(event) => setGenerationDate(event.target.value)} /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "生成中…" : "生成任务"}</button></div></form>}
    {activeForm?.type === "edit-plan" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); const plan = activeForm.plan; void runAction(() => careApi.updatePlan(plan.id, { ...planForm, planVersion: plan.planVersion, startDate: planForm.startDate, endDate: planForm.endDate || undefined }), "护理计划已更新"); }}><div className="panel-heading compact"><div><h2>编辑护理计划</h2><p>已发布计划只能按服务端规则维护版本。</p></div></div><div className="record-fields"><label>服务长者<input readOnly value={planForm.residentId} /></label><label>计划名称<input required value={planForm.planName} onChange={(event) => setPlanForm((value) => ({ ...value, planName: event.target.value }))} /></label><label>服务频次<input required value={planForm.frequencyText} onChange={(event) => setPlanForm((value) => ({ ...value, frequencyText: event.target.value }))} /></label><label>开始日期<input required type="date" value={planForm.startDate} onChange={(event) => setPlanForm((value) => ({ ...value, startDate: event.target.value }))} /></label><label>结束日期<input type="date" value={planForm.endDate} onChange={(event) => setPlanForm((value) => ({ ...value, endDate: event.target.value }))} /></label></div><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "保存中" : "保存修改"}</button></div></form>}
    <div className="care-execution-grid"><section className="panel execution-panel"><div className="panel-heading"><div><h2>今日执行队列</h2><p>任务完成必须留下服务记录；异常将自动创建跟进项。</p></div><button type="button" className="text-action" onClick={() => setActiveForm({ type: "task" })}><Plus size={15} />手工任务</button></div><div className="execution-list">{tasks.length ? tasks.map((task) => <article key={task.id}><div className="execution-time"><i className={task.level} />{task.time}</div><div className="execution-task"><b>{task.task}</b><span>{task.name} · {task.owner}</span></div><StatusPill status={task.status} />{task.statusCode === "PENDING" ? <span className="row-actions"><button type="button" title="开始执行" onClick={() => void runAction(() => careApi.startTask(task.id, { taskVersion: task.taskVersion }), "护理任务已开始")}>开始</button><button type="button" title="完成并记录服务" onClick={() => setActiveForm({ type: "complete", taskId: task.id })}><Check size={16} /></button><button type="button" title="标记异常" onClick={() => setActiveForm({ type: "exception", taskId: task.id })}><AlertTriangle size={16} /></button></span> : task.statusCode === "IN_PROGRESS" || task.statusCode === "PENDING_CONFIRMATION" ? <span className="row-actions"><button type="button" title="完成并记录服务" onClick={() => setActiveForm({ type: "complete", taskId: task.id })}><Check size={16} /></button><button type="button" title="标记异常" onClick={() => setActiveForm({ type: "exception", taskId: task.id })}><AlertTriangle size={16} /></button></span> : null}</article>) : <p className="empty-inline">暂无任务，可发布计划后按模板生成。</p>}</div></section><aside className="panel plan-panel"><div className="panel-heading"><div><h2>护理计划与模板</h2><p>草稿发布后成为长者当前生效版本</p></div><span className="detail-count">{plans.length} 个</span></div><div className="care-plan-list">{plans.map((plan) => <article key={plan.id}><span className="plan-icon"><HeartPulse size={16} /></span><div><b>{plan.plan}</b><small>{plan.name} · {plan.frequency}</small><span>{plan.owner} · {plan.status}</span></div><button type="button" aria-label={`查看${plan.plan}模板`} onClick={() => void loadTemplates(plan.id)}><ChevronRight size={17} /></button>{plan.statusCode === "DRAFT" && <><button type="button" className="text-action" onClick={() => setActiveForm({ type: "edit-plan", plan })}>编辑</button><button type="button" className="text-action" onClick={() => void runAction(() => careApi.publishPlan(plan.id, plan.planVersion), "护理计划已发布")}>发布</button></>}{plan.statusCode === "PUBLISHED" && <button type="button" className="text-action danger" onClick={() => void runAction(() => careApi.cancelPlan(plan.id, plan.planVersion), "护理计划已停用")}>停用</button>}</article>)}</div></aside></div>
    {activeForm?.type === "task" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); void runAction(() => careApi.createTask({ ...taskForm, planId: Number(taskForm.planId), assigneeId: Number(taskForm.assigneeId) }), "护理任务已创建"); }}><div className="panel-heading compact"><div><h2>新建护理任务</h2><p>只允许引用当前已发布计划</p></div></div><div className="record-fields"><label>护理计划<select required value={taskForm.planId} onChange={(event) => setTaskForm((value) => ({ ...value, planId: event.target.value }))}><option value="">请选择</option>{plans.filter((plan) => plan.statusCode === "PUBLISHED").map((plan) => <option key={plan.id} value={plan.id}>{plan.plan} · {plan.name}</option>)}</select></label><label>任务名称<input required value={taskForm.taskName} onChange={(event) => setTaskForm((value) => ({ ...value, taskName: event.target.value }))} /></label><label>执行时间<input required type="datetime-local" value={taskForm.scheduledAt} onChange={(event) => setTaskForm((value) => ({ ...value, scheduledAt: event.target.value }))} /></label><label>执行成员 ID<input required type="number" min="1" value={taskForm.assigneeId} onChange={(event) => setTaskForm((value) => ({ ...value, assigneeId: event.target.value }))} /></label></div><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "创建中…" : "创建任务"}</button></div></form>}
    {taskForForm && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); const formData = new FormData(event.currentTarget); const note = String(formData.get("note") || "").trim(); if (!note) return; void runAction(() => activeForm.type === "complete" ? careApi.completeTask(taskForForm.id, { taskVersion: taskForForm.taskVersion, resultNote: note }) : careApi.markTaskException(taskForForm.id, { taskVersion: taskForForm.taskVersion, exceptionReason: note }), activeForm.type === "complete" ? "任务已完成，服务记录已同步" : "任务已标记异常并创建跟进项"); }}><div className="panel-heading compact"><div><h2>{activeForm.type === "complete" ? "完成护理任务" : "标记任务异常"}</h2><p>{taskForForm.task}</p></div></div><label>{activeForm.type === "complete" ? "服务记录" : "异常说明"}<textarea required name="note" /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className={activeForm.type === "complete" ? "button primary" : "button destructive"} disabled={working}>{working ? "提交中…" : "确认提交"}</button></div></form>}
    <div className="care-execution-grid"><section className="panel execution-panel"><div className="panel-heading"><div><h2>异常跟进</h2><p>开放异常必须结案后才能提交关联交接。</p></div></div><div className="execution-list">{followUps.length ? followUps.map((followUp) => <article key={followUp.id}><div className="execution-task"><b>任务 #{followUp.taskId} · 长者 #{followUp.residentId}</b><span>{followUp.exceptionReason}</span></div><StatusPill status={followUp.status === "RESOLVED" ? "已完成" : "待确认"} />{followUp.status === "OPEN" && <button type="button" className="button secondary" onClick={() => setActiveForm({ type: "resolve", followUp })}>结案</button>}</article>) : <p className="empty-inline">暂无开放跟进项</p>}</div></section><aside className="panel plan-panel"><div className="panel-heading"><div><h2>班次交接</h2><p>仅可提交不存在开放异常的任务集合</p></div><button type="button" className="text-action" onClick={() => setActiveForm({ type: "handover" })}><Plus size={15} />新建</button></div><div className="care-plan-list">{handovers.map((handover) => <article key={handover.id}><div><b>{handover.shiftDate} · {handover.shiftCode}</b><small>接班成员 #{handover.toUserId} · {handover.status}</small></div>{handover.status === "DRAFT" && <button type="button" className="text-action" onClick={() => void runAction(() => careApi.submitShiftHandover(handover.id, handover.version), "班次交接已提交")}>提交</button>}</article>)}</div></aside></div>
    {selectedPlanId && <section className="panel resident-gateway-form"><div className="panel-heading compact"><div><h2>计划 #{selectedPlanId} 的任务模板</h2><p>模板用于按服务日期生成护理任务</p></div><button type="button" className="text-action" onClick={() => setActiveForm({ type: "template" })}><Plus size={15} />添加模板</button></div>{templates.length ? <div className="execution-list">{templates.map((template) => <article key={template.id}><div className="execution-task"><b>{template.taskName}</b><span>{template.scheduledTime} · 成员 #{template.assigneeId}</span></div><StatusPill status={template.active ? "已完成" : "待确认"} /></article>)}</div> : <p className="empty-inline">暂无模板</p>}</section>}
    {activeForm?.type === "template" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); void runAction(async () => { await careApi.createTaskTemplate({ ...templateForm, planId: Number(selectedPlanId), assigneeId: Number(templateForm.assigneeId) }); await loadTemplates(selectedPlanId); }, "任务模板已创建"); }}><div className="panel-heading compact"><div><h2>添加任务模板</h2><p>模板将归入当前选择的护理计划</p></div></div><div className="record-fields"><label>任务名称<input required value={templateForm.taskName} onChange={(event) => setTemplateForm((value) => ({ ...value, taskName: event.target.value }))} /></label><label>计划时间<input required type="time" value={templateForm.scheduledTime} onChange={(event) => setTemplateForm((value) => ({ ...value, scheduledTime: event.target.value }))} /></label><label>执行成员 ID<input required type="number" min="1" value={templateForm.assigneeId} onChange={(event) => setTemplateForm((value) => ({ ...value, assigneeId: event.target.value }))} /></label></div><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "保存中…" : "保存模板"}</button></div></form>}
    {activeForm?.type === "resolve" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); const formData = new FormData(event.currentTarget); const resolutionNote = String(formData.get("resolutionNote") || "").trim(); if (!resolutionNote) return; void runAction(() => careApi.resolveTaskFollowUp(activeForm.followUp.id, { followUpVersion: activeForm.followUp.version, resolutionNote }), "异常跟进项已结案"); }}><div className="panel-heading compact"><div><h2>结案异常跟进</h2><p>{activeForm.followUp.exceptionReason}</p></div></div><label>结案说明<textarea required name="resolutionNote" /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "结案中…" : "确认结案"}</button></div></form>}
    {activeForm?.type === "handover" && <form className="panel resident-gateway-form" onSubmit={(event) => { event.preventDefault(); if (!handoverForm.taskIds.length) { onAnnounce("请至少选择一项已完成或异常任务"); return; } void runAction(() => careApi.createShiftHandover({ ...handoverForm, toUserId: Number(handoverForm.toUserId) }), "班次交接草稿已创建"); }}><div className="panel-heading compact"><div><h2>新建班次交接</h2><p>仅可选已完成或已说明异常的任务</p></div></div><div className="record-fields"><label>交接日期<input required type="date" value={handoverForm.shiftDate} onChange={(event) => setHandoverForm((value) => ({ ...value, shiftDate: event.target.value }))} /></label><label>班次代码<input required value={handoverForm.shiftCode} onChange={(event) => setHandoverForm((value) => ({ ...value, shiftCode: event.target.value }))} /></label><label>接班成员 ID<input required type="number" min="1" value={handoverForm.toUserId} onChange={(event) => setHandoverForm((value) => ({ ...value, toUserId: event.target.value }))} /></label></div><label>交接说明<textarea required value={handoverForm.note} onChange={(event) => setHandoverForm((value) => ({ ...value, note: event.target.value }))} /></label><fieldset><legend>交接任务</legend>{eligibleHandoverTasks.map((task) => <label key={task.id}><input type="checkbox" checked={handoverForm.taskIds.includes(task.id)} onChange={(event) => setHandoverForm((value) => ({ ...value, taskIds: event.target.checked ? [...value.taskIds, task.id] : value.taskIds.filter((id) => id !== task.id) }))} />{task.task} · {task.name}</label>)}</fieldset><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setActiveForm(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "创建中…" : "创建交接草稿"}</button></div></form>}
  </section>;
}

function AdmissionDispatchWorkspace({ records, onCreate, onAnnounce }) {
  const [admissions, setAdmissions] = useState(records);
  const [bedFilter, setBedFilter] = useState("全部");
  const [selected, setSelected] = useState(null);
  const beds = [{ id: "A-205-02", room: "A-205", type: "双人照护房", status: "空闲", note: "靠窗 · 可立即入住" }, { id: "B-306-02", room: "B-306", type: "双人照护房", status: "清洁中", note: "14:30 完成清洁" }, { id: "C-108-01", room: "C-108", type: "康复护理房", status: "空闲", note: "适配术后康复" }, { id: "A-119-01", room: "A-119", type: "认知照护房", status: "已预留", note: "预留至 07-30 12:00" }];
  const candidates = admissions.filter((record) => record.status === "待评估");
  const visibleBeds = bedFilter === "全部" ? beds : beds.filter((bed) => bed.status === bedFilter);
  const assignBed = (bed) => {
    if (!selected) { onAnnounce("请先选择一位待安排长者"); return; }
    setAdmissions((current) => current.map((record) => record.id === selected.id ? { ...record, room: bed.room, bed: bed.id, status: "待确认入住" } : record));
    setSelected(null);
    onAnnounce(`${selected.name} 已分配至 ${bed.id}，等待入住确认`);
  };
  return <section className="admission-workspace"><div className="module-hero"><div><p className="eyebrow"><span />入住与床位调度</p><h1>入住调度中心</h1><p>将评估结果、床位状态与入住确认集中处理，避免跨班遗漏。</p></div><button type="button" className="button primary" onClick={() => onCreate("admission")}><Plus size={17} />发起入住评估</button></div><section className="dispatch-kpis"><article><span>待评估</span><strong>{candidates.length}</strong><small>含 1 项今日预约</small></article><article><span>可立即入住</span><strong>34</strong><small>已通过床位与卫生状态校验</small></article><article><span>待确认入住</span><strong>{admissions.filter((record) => record.status === "待确认入住").length}</strong><small>需由前台与护理共同确认</small></article></section><div className="dispatch-grid"><section className="panel dispatch-candidates"><div className="panel-heading"><div><h2>待安排长者</h2><p>完成评估后选择合适房间与床位</p></div><span className="detail-count">{candidates.length} 位</span></div><div className="candidate-list">{candidates.map((record) => <button type="button" key={record.id} className={selected?.id === record.id ? "candidate-row active" : "candidate-row"} onClick={() => setSelected(record)}><span className="candidate-avatar">{record.name.slice(0, 1)}</span><span><b>{record.name}</b><small>{record.status} · 申请入住 {record.date}</small></span><span className="candidate-action">{selected?.id === record.id ? "已选择" : "选择"}</span></button>)}</div><div className="dispatch-note"><ClipboardCheck size={16} /><span>已选长者：<b>{selected ? selected.name : "尚未选择"}</b></span></div></section><section className="panel bed-panel"><div className="panel-heading"><div><h2>床位调度</h2><p>仅展示当前机构可分配的房间资源</p></div><button type="button" className="text-action" onClick={() => setBedFilter("全部")}>清除筛选</button></div><div className="filter-row">{["全部", "空闲", "清洁中", "已预留"].map((item) => <button type="button" key={item} className={bedFilter === item ? "filter active" : "filter"} onClick={() => setBedFilter(item)}>{item}</button>)}</div><div className="bed-grid">{visibleBeds.map((bed) => <article key={bed.id} className={`bed-card ${bed.status === "空闲" ? "available" : ""}`}><div><span>{bed.room}</span><StatusPill status={bed.status === "空闲" ? "已完成" : bed.status === "清洁中" ? "进行中" : "待确认"} /></div><b>{bed.id}</b><small>{bed.type}</small><p>{bed.note}</p><button type="button" disabled={bed.status !== "空闲"} className={bed.status === "空闲" ? "button primary" : "button secondary"} onClick={() => assignBed(bed)}>{bed.status === "空闲" ? "分配此床位" : bed.status}</button></article>)}</div></section></div></section>;
}

const ADMISSION_STATUS_LABELS = {
  PENDING_ASSESSMENT: "待评估",
  ASSESSMENT_REJECTED: "评估未通过",
  PENDING_ASSIGNMENT: "待安排",
  PENDING_CONFIRMATION: "待确认入住",
  ADMITTED: "已入住",
  DISCHARGED: "已退住",
  CANCELLED: "已取消",
};

const BED_STATUS_LABELS = {
  AVAILABLE: "空闲",
  RESERVED: "已预留",
  OCCUPIED: "已占用",
  CLEANING: "清洁中",
  MAINTENANCE: "维护中",
};

function toGatewayAdmissionRecord(source) {
  return {
    id: source.id,
    name: source.residentName || `长者 #${source.residentId}`,
    room: source.roomNo || "待分配",
    bed: source.bedNo || "未分配",
    date: source.appliedAt ? String(source.appliedAt).slice(0, 10) : "-",
    rawStatus: source.status,
    status: ADMISSION_STATUS_LABELS[source.status] || source.status,
    admissionVersion: source.version,
    bedVersion: source.bedVersion,
    assessmentDecision: source.assessmentDecision,
    assessmentConclusion: source.assessmentConclusion,
    assessedAt: source.assessedAt,
  };
}

function toGatewayBedRecord(source) {
  const status = source.hygieneStatus !== "READY" ? "清洁中" : (BED_STATUS_LABELS[source.occupancyStatus] || source.occupancyStatus);
  return {
    id: source.id,
    room: source.roomNo,
    type: source.roomType,
    bed: source.bedNo,
    status,
    version: source.version,
    note: source.hygieneStatus === "READY" ? "卫生已准备" : "等待卫生准备完成",
  };
}

function GatewayAdmissionPage({ admissionApi, residentApi, onAnnounce }) {
  const [residents, setResidents] = useState([]);
  const [admissions, setAdmissions] = useState([]);
  const [residentId, setResidentId] = useState("");
  const [cancelTarget, setCancelTarget] = useState("");
  const [working, setWorking] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(async () => {
    try {
      const [residentResult, admissionResult] = await Promise.all([
        residentApi.listResidents(),
        admissionApi.listAdmissions(),
      ]);
      setResidents(residentResult.filter((resident) => !["ARCHIVED", "DECEASED"].includes(resident.status)));
      setAdmissions(admissionResult.filter((admission) => ["PENDING_ASSESSMENT", "ASSESSMENT_REJECTED", "PENDING_ASSIGNMENT", "PENDING_CONFIRMATION"].includes(admission.status)));
    } catch (error) {
      onAnnounce(error.message || "无法读取入住申请所需的当前机构数据");
    }
  }, [admissionApi, onAnnounce, residentApi]);

  useEffect(() => {
    void refresh();
  }, [refresh, refreshKey]);

  const createAdmission = async (event) => {
    event.preventDefault();
    if (!residentId) return;
    setWorking(true);
    try {
      await admissionApi.createAdmission(Number(residentId));
      setResidentId("");
      setRefreshKey((value) => value + 1);
      onAnnounce("入住申请已创建，等待评估后安排床位");
    } catch (error) {
      onAnnounce(error.message || "入住申请创建失败");
    } finally {
      setWorking(false);
    }
  };

  const cancelAdmission = async (event) => {
    event.preventDefault();
    const target = admissions.find((admission) => String(admission.id) === cancelTarget);
    if (!target) return;
    setWorking(true);
    try {
      await admissionApi.cancel(target.id, { admissionVersion: target.version, bedVersion: target.bedVersion });
      setCancelTarget("");
      setRefreshKey((value) => value + 1);
      onAnnounce("入住申请已取消；如存在床位预留，已由服务端一并释放");
    } catch (error) {
      onAnnounce(error.status === 409 ? "入住申请或床位已变化，已刷新最新状态" : (error.message || "取消入住申请失败"));
      setRefreshKey((value) => value + 1);
    } finally {
      setWorking(false);
    }
  };

  return <><section className="panel admission-entry-panel"><div className="panel-heading"><div><h2>入住申请</h2><p>仅选择当前机构内可用的长者档案；床位预留与取消均由服务端状态机处理。</p></div></div><div className="admission-entry-forms"><form onSubmit={createAdmission}><label>申请长者<select required value={residentId} onChange={(event) => setResidentId(event.target.value)}><option value="">请选择长者</option>{residents.map((resident) => <option key={resident.id} value={resident.id}>{resident.name} · {resident.status}</option>)}</select></label><button type="submit" className="button primary" disabled={working || !residentId}><Plus size={16} />创建申请</button></form><form onSubmit={cancelAdmission}><label>取消申请<select required value={cancelTarget} onChange={(event) => setCancelTarget(event.target.value)}><option value="">请选择未完成申请</option>{admissions.map((admission) => <option key={admission.id} value={admission.id}>{admission.residentName || `长者 #${admission.residentId}`} · {ADMISSION_STATUS_LABELS[admission.status] || admission.status}</option>)}</select></label><button type="submit" className="button destructive" disabled={working || !cancelTarget}>取消申请</button></form></div></section><GatewayAdmissionDispatchWorkspace admissionApi={admissionApi} onAnnounce={onAnnounce} refreshKey={refreshKey} /></>;
}

function GatewayAdmissionDispatchWorkspace({ admissionApi, onAnnounce, refreshKey }) {
  const [admissions, setAdmissions] = useState([]);
  const [beds, setBeds] = useState([]);
  const [bedFilter, setBedFilter] = useState("全部");
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState(null);
  const [assessmentTarget, setAssessmentTarget] = useState(null);
  const [assessmentConclusion, setAssessmentConclusion] = useState("");
  const [assessmentDecision, setAssessmentDecision] = useState("PASSED");
  const [transferTarget, setTransferTarget] = useState(null);
  const [transferReason, setTransferReason] = useState("");
  const [cleaningTarget, setCleaningTarget] = useState(null);
  const [cleaningResult, setCleaningResult] = useState("");
  const [dischargeTarget, setDischargeTarget] = useState(null);
  const [dischargeReason, setDischargeReason] = useState("");
  const idempotencyKeysRef = useRef(new Map());
  const onCreate = () => onAnnounce("请先在上方创建入住申请，再录入评估结论");

  const idempotencyKeyFor = (scope) => {
    const existing = idempotencyKeysRef.current.get(scope);
    if (existing) return existing;
    const key = crypto.randomUUID();
    idempotencyKeysRef.current.set(scope, key);
    return key;
  };

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [admissionResult, bedResult] = await Promise.all([
        admissionApi.listAdmissions(),
        admissionApi.listBeds(),
      ]);
      setAdmissions(admissionResult.map(toGatewayAdmissionRecord));
      setBeds(bedResult.map(toGatewayBedRecord));
    } catch (error) {
      onAnnounce(error.message || "入住调度数据读取失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  }, [admissionApi, onAnnounce]);

  useEffect(() => {
    void refresh();
  }, [refresh, refreshKey]);

  const assessments = admissions.filter((record) => ["PENDING_ASSESSMENT", "ASSESSMENT_REJECTED"].includes(record.rawStatus));
  const candidates = admissions.filter((record) => record.rawStatus === "PENDING_ASSIGNMENT");
  const confirmations = admissions.filter((record) => record.rawStatus === "PENDING_CONFIRMATION");
  const admittedRecords = admissions.filter((record) => record.rawStatus === "ADMITTED");
  const visibleBeds = bedFilter === "全部" ? beds : beds.filter((bed) => bed.status === bedFilter);

  const handleConflict = async (error, idempotencyScope) => {
    if (error.status === 409) {
      idempotencyKeysRef.current.delete(idempotencyScope);
      onAnnounce("床位或入住单已变化，已刷新最新状态");
      await refresh();
      return;
    }
    onAnnounce(error.message || "操作未完成，请稍后重试");
  };

  const decideAssessment = async (event) => {
    event.preventDefault();
    if (!assessmentTarget || !assessmentConclusion.trim()) return;
    const scope = `assessment:${assessmentTarget.id}:${assessmentTarget.admissionVersion}:${assessmentConclusion}`;
    setWorkingId(`assessment-${assessmentTarget.id}`);
    try {
      await admissionApi.decideAssessment(assessmentTarget.id, {
        admissionVersion: assessmentTarget.admissionVersion,
        decision: assessmentDecision,
        conclusion: assessmentConclusion.trim(),
      }, idempotencyKeyFor(scope));
      idempotencyKeysRef.current.delete(scope);
      setAssessmentTarget(null);
      setAssessmentConclusion("");
      await refresh();
      onAnnounce(assessmentDecision === "PASSED" ? "入住评估已通过，可继续安排床位" : "入住评估未通过，申请保留在评估队列");
    } catch (error) {
      await handleConflict(error, scope);
    } finally {
      setWorkingId(null);
    }
  };

  const transferBed = async (bed) => {
    if (!transferTarget || !transferReason.trim()) return;
    const scope = `transfer:${transferTarget.id}:${transferTarget.admissionVersion}:${transferTarget.bedVersion}:${bed.id}:${bed.version}:${transferReason}`;
    setWorkingId(`transfer-${bed.id}`);
    try {
      await admissionApi.transferBed(transferTarget.id, {
        targetBedId: bed.id,
        admissionVersion: transferTarget.admissionVersion,
        sourceBedVersion: transferTarget.bedVersion,
        targetBedVersion: bed.version,
        reason: transferReason.trim(),
      }, idempotencyKeyFor(scope));
      idempotencyKeysRef.current.delete(scope);
      setTransferTarget(null);
      setTransferReason("");
      await refresh();
      onAnnounce(`${transferTarget.name} 已完成调床，原床位已转入清洁`);
    } catch (error) {
      await handleConflict(error, scope);
    } finally {
      setWorkingId(null);
    }
  };

  const assignBed = async (bed) => {
    if (!selected) {
      onAnnounce("请先选择一位待安排长者");
      return;
    }
    const idempotencyScope = `assign:${selected.id}:${selected.admissionVersion}:${bed.id}:${bed.version}`;
    const idempotencyKey = idempotencyKeyFor(idempotencyScope);
    setWorkingId(`assign-${bed.id}`);
    try {
      await admissionApi.assignBed(selected.id, {
        bedId: bed.id,
        admissionVersion: selected.admissionVersion,
        bedVersion: bed.version,
      }, idempotencyKey);
      idempotencyKeysRef.current.delete(idempotencyScope);
      setSelected(null);
      await refresh();
      onAnnounce(`${selected.name} 已预留至 ${bed.bed}，等待入住确认`);
    } catch (error) {
      await handleConflict(error, idempotencyScope);
    } finally {
      setWorkingId(null);
    }
  };

  const confirmAdmission = async (record) => {
    if (record.bedVersion == null) {
      onAnnounce("当前入住单缺少床位版本，请刷新后重试");
      return;
    }
    const idempotencyScope = `confirm:${record.id}:${record.admissionVersion}:${record.bedVersion}`;
    const idempotencyKey = idempotencyKeyFor(idempotencyScope);
    setWorkingId(`confirm-${record.id}`);
    try {
      await admissionApi.confirm(record.id, {
        admissionVersion: record.admissionVersion,
        bedVersion: record.bedVersion,
      }, idempotencyKey);
      idempotencyKeysRef.current.delete(idempotencyScope);
      await refresh();
      onAnnounce(`${record.name} 已确认入住`);
    } catch (error) {
      await handleConflict(error, idempotencyScope);
    } finally {
      setWorkingId(null);
    }
  };

  const dischargeAdmission = async (event) => {
    event.preventDefault();
    if (!dischargeTarget || dischargeTarget.bedVersion == null) {
      onAnnounce("当前入住单缺少床位版本，请刷新后重试");
      return;
    }
    const idempotencyScope = `discharge:${dischargeTarget.id}:${dischargeTarget.admissionVersion}:${dischargeTarget.bedVersion}:${dischargeReason}`;
    const idempotencyKey = idempotencyKeyFor(idempotencyScope);
    setWorkingId(`discharge-${dischargeTarget.id}`);
    try {
      await admissionApi.discharge(dischargeTarget.id, {
        admissionVersion: dischargeTarget.admissionVersion,
        bedVersion: dischargeTarget.bedVersion,
        dischargeReason,
      }, idempotencyKey);
      idempotencyKeysRef.current.delete(idempotencyScope);
      const residentName = dischargeTarget.name;
      setDischargeTarget(null);
      setDischargeReason("");
      await refresh();
      onAnnounce(`${residentName} 已办理退住，床位进入清洁状态`);
    } catch (error) {
      await handleConflict(error, idempotencyScope);
    } finally {
      setWorkingId(null);
    }
  };

  const completeBedCleaning = async (bed) => {
    if (!bed) return;
    const result = cleaningResult.trim() || "已完成床单位及床旁清洁消毒";
    setWorkingId(`clean-${bed.id}`);
    try {
      await admissionApi.completeBedCleaning(bed.id, { bedVersion: bed.version, result });
      setCleaningTarget(null);
      setCleaningResult("");
      await refresh();
      onAnnounce(`${bed.room} ${bed.bed} 已完成清洁，可再次分配`);
    } catch (error) {
      await handleConflict(error, `clean:${bed.id}:${bed.version}`);
    } finally {
      setWorkingId(null);
    }
  };

  return <section className="admission-workspace"><div className="module-hero"><div><p className="eyebrow"><span />入住与床位调度</p><h1>入住调度中心</h1><p>评估、分配、确认、调床、退住与清洁均由当前机构的服务端状态机执行。</p></div></div><section className="dispatch-kpis"><article><span>待评估</span><strong>{assessments.length}</strong><small>通过后才可安排床位</small></article><article><span>待安排</span><strong>{candidates.length}</strong><small>已评估通过</small></article><article><span>待确认</span><strong>{confirmations.length}</strong><small>预留未过期</small></article></section><div className="dispatch-grid"><section className="panel dispatch-candidates"><div className="panel-heading"><div><h2>入住决策与在院长者</h2><p>每一步均使用当前版本，冲突后自动刷新。</p></div></div><div className="candidate-list">{assessments.map((record) => <div key={record.id} className="candidate-row"><span className="candidate-avatar">{record.name.slice(0, 1)}</span><span><b>{record.name}</b><small>{record.status} · {record.assessmentConclusion || "待录入评估结论"}</small></span><button type="button" className="button secondary" onClick={() => { setAssessmentTarget(record); setAssessmentConclusion(record.assessmentConclusion || ""); }}>录入评估</button></div>)}</div>{assessmentTarget && <form className="admission-discharge-form" onSubmit={decideAssessment}><label>评估结论<textarea required maxLength="500" value={assessmentConclusion} onChange={(event) => setAssessmentConclusion(event.target.value)} /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setAssessmentTarget(null)}>取消</button><button type="submit" className="button primary" disabled={workingId === `assessment-${assessmentTarget.id}`}>通过并安排床位</button></div></form>}<div className="panel-heading compact"><div><h2>待安排长者</h2><p>选择后可分配卫生已准备的空闲床位。</p></div></div><div className="candidate-list">{candidates.map((record) => <button type="button" key={record.id} className={selected?.id === record.id ? "candidate-row active" : "candidate-row"} onClick={() => setSelected(record)}><span className="candidate-avatar">{record.name.slice(0, 1)}</span><span><b>{record.name}</b><small>申请入住 {record.date}</small></span><span className="candidate-action">{selected?.id === record.id ? "已选择" : "选择"}</span></button>)}</div>{confirmations.map((record) => <div key={record.id} className="candidate-row"><span><b>{record.name}</b><small>{record.room} · {record.bed}</small></span><button type="button" className="button secondary" onClick={() => confirmAdmission(record)}>确认入住</button></div>)}{admittedRecords.map((record) => <div key={record.id} className="candidate-row"><span><b>{record.name}</b><small>{record.room} · {record.bed}</small></span><button type="button" className="button secondary" onClick={() => { setTransferTarget(record); setTransferReason(""); }}>调床</button><button type="button" className="button secondary" onClick={() => { setDischargeTarget(record); setDischargeReason(""); }}>退住</button></div>)}{transferTarget && <form className="admission-discharge-form"><label>调床理由<textarea required maxLength="500" value={transferReason} onChange={(event) => setTransferReason(event.target.value)} /></label><small>选择右侧空闲床位完成调床。</small><button type="button" className="text-action" onClick={() => setTransferTarget(null)}>取消调床</button></form>}{dischargeTarget && <form className="admission-discharge-form" onSubmit={dischargeAdmission}><label>退住原因<textarea required maxLength="500" value={dischargeReason} onChange={(event) => setDischargeReason(event.target.value)} /></label><button type="submit" className="button destructive">确认退住</button></form>}</section><section className="panel bed-panel"><div className="panel-heading"><div><h2>床位资源</h2><p>{transferTarget ? `正在为 ${transferTarget.name} 选择目标床位` : "先选择待安排长者，再分配空闲床位"}</p></div><button type="button" className="text-action" onClick={() => void refresh()}>刷新</button></div><div className="bed-grid">{beds.map((bed) => <article key={bed.id} className={`bed-card ${bed.status === "空闲" ? "available" : ""}`}><span>{bed.room}</span><b>{bed.bed}</b><small>{bed.type} · {bed.status}</small>{bed.status === "空闲" ? <button type="button" className="button primary" disabled={workingId === `assign-${bed.id}` || (transferTarget && !transferReason.trim())} onClick={() => transferTarget ? transferBed(bed) : assignBed(bed)}>{transferTarget ? "调至此床位" : "分配此床位"}</button> : bed.status === "清洁中" ? <button type="button" className="button secondary" onClick={() => completeBedCleaning(bed)}>确认清洁完成</button> : null}</article>)}</div></section></div></section>;

  return <section className="admission-workspace"><div className="module-hero"><div><p className="eyebrow"><span />入住与床位调度</p><h1>入住调度中心</h1><p>当前显示所选租户的真实入住单与床位状态，分配和确认均使用版本校验。</p></div><button type="button" className="button primary" onClick={() => onCreate("admission")}><Plus size={17} />发起入住评估</button></div><section className="dispatch-kpis"><article><span>待安排</span><strong>{candidates.length}</strong><small>已完成评估，等待床位分配</small></article><article><span>可立即入住</span><strong>{beds.filter((bed) => bed.status === "空闲").length}</strong><small>卫生状态已准备</small></article><article><span>待确认入住</span><strong>{confirmations.length}</strong><small>需由前台与护理共同确认</small></article></section><div className="dispatch-grid"><section className="panel dispatch-candidates"><div className="panel-heading"><div><h2>待安排长者</h2><p>{loading ? "正在读取当前机构数据…" : "选择长者后分配空闲床位"}</p></div><span className="detail-count">{candidates.length} 位</span></div><div className="candidate-list">{candidates.length ? candidates.map((record) => <button type="button" key={record.id} className={selected?.id === record.id ? "candidate-row active" : "candidate-row"} onClick={() => setSelected(record)}><span className="candidate-avatar">{record.name.slice(0, 1)}</span><span><b>{record.name}</b><small>{record.status} · 申请入住 {record.date}</small></span><span className="candidate-action">{selected?.id === record.id ? "已选择" : "选择"}</span></button>) : <p className="empty-inline">{loading ? "正在读取…" : "暂无待安排长者"}</p>}</div><div className="dispatch-note"><ClipboardCheck size={16} /><span>已选长者：<b>{selected ? selected.name : "尚未选择"}</b></span></div>{confirmations.length > 0 && <><div className="panel-heading compact"><div><h2>待确认入住</h2><p>确认后将同步占用床位与长者在院状态</p></div></div><div className="candidate-list">{confirmations.map((record) => <div key={record.id} className="candidate-row"><span className="candidate-avatar">{record.name.slice(0, 1)}</span><span><b>{record.name}</b><small>{record.room} · {record.bed}</small></span><button type="button" className="button secondary" disabled={workingId === `confirm-${record.id}`} onClick={() => confirmAdmission(record)}>{workingId === `confirm-${record.id}` ? "确认中…" : "确认入住"}</button></div>)}</div></>}{admittedRecords.length > 0 && <><div className="panel-heading compact"><div><h2>在院长者</h2><p>退住后将解除床位绑定并进入清洁状态</p></div></div><div className="candidate-list">{admittedRecords.map((record) => <div key={record.id} className="candidate-row"><span className="candidate-avatar">{record.name.slice(0, 1)}</span><span><b>{record.name}</b><small>{record.room} · {record.bed}</small></span><button type="button" className="button secondary" onClick={() => { setDischargeTarget(record); setDischargeReason(""); }}>办理退住</button></div>)}</div>{dischargeTarget && <form className="admission-discharge-form" onSubmit={dischargeAdmission}><label>退住原因<textarea required maxLength="500" value={dischargeReason} onChange={(event) => setDischargeReason(event.target.value)} /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setDischargeTarget(null)}>取消</button><button type="submit" className="button destructive" disabled={workingId === `discharge-${dischargeTarget.id}`}>{workingId === `discharge-${dischargeTarget.id}` ? "提交中…" : "确认退住"}</button></div></form>}</>}</section><section className="panel bed-panel"><div className="panel-heading"><div><h2>床位调度</h2><p>仅展示当前机构范围内的房间资源</p></div><button type="button" className="text-action" onClick={() => void refresh()}>刷新数据</button></div><div className="filter-row">{["全部", "空闲", "清洁中", "已预留", "已占用"].map((item) => <button type="button" key={item} className={bedFilter === item ? "filter active" : "filter"} onClick={() => setBedFilter(item)}>{item}</button>)}</div><div className="bed-grid">{visibleBeds.map((bed) => <article key={bed.id} className={`bed-card ${bed.status === "空闲" ? "available" : ""}`}><div><span>{bed.room}</span><StatusPill status={bed.status === "空闲" ? "已完成" : bed.status === "清洁中" ? "进行中" : "待确认"} /></div><b>{bed.bed}</b><small>{bed.type}</small><p>{bed.note}</p>{bed.status === "空闲" ? <button type="button" disabled={workingId === `assign-${bed.id}`} className="button primary" onClick={() => assignBed(bed)}>{workingId === `assign-${bed.id}` ? "分配中…" : "分配此床位"}</button> : bed.status === "清洁中" ? <button type="button" disabled={workingId === `clean-${bed.id}`} className="button secondary" onClick={() => completeBedCleaning(bed)}>{workingId === `clean-${bed.id}` ? "确认中…" : "确认清洁完成"}</button> : <button type="button" disabled className="button secondary">{bed.status}</button>}</article>)}</div></section></div></section>;
}

function DeviceCenterWorkspace({ records, onCreate, onAnnounce }) {
  const [view, setView] = useState("overview");
  const [deviceFilter, setDeviceFilter] = useState("全部");
  const [alertFilter, setAlertFilter] = useState("全部");
  const [maintenance, setMaintenance] = useState([{ id: 1, device: "紧急呼叫按钮", location: "C-403 卫生间", type: "低电量更换", due: "今日 11:30", owner: "工程组", status: "待维护" }, { id: 2, device: "电子围栏", location: "康复花园", type: "通讯模块巡检", due: "今日 16:00", owner: "设备专员", status: "处理中" }, { id: 3, device: "睡眠监测带", location: "B-208 房", type: "月度清洁与校准", due: "2026-08-02", owner: "护理组", status: "计划中" }]);
  const [incidents, setIncidents] = useState([{ id: 1, device: "紧急呼叫按钮", location: "C-403 卫生间", type: "低电量", level: "高优先级", status: "待确认", time: "09:12" }, { id: 2, device: "电子围栏", location: "康复花园", type: "通讯异常", level: "中优先级", status: "处理中", time: "08:45" }, { id: 3, device: "睡眠监测带", location: "B-208 房", type: "离线 8 分钟", level: "一般", status: "待确认", time: "08:26" }]);
  const usage = { 401: "今日 12 次呼叫测试", 402: "昨夜监测 8 小时", 403: "今日活动识别 16 次", 404: "今日围栏通行 31 次" };
  const inspection = { 401: "2026-07-22", 402: "2026-07-28", 403: "2026-07-26", 404: "2026-07-18" };
  const visibleDevices = deviceFilter === "全部" ? records : records.filter((item) => item.status === deviceFilter);
  const visibleAlerts = alertFilter === "全部" ? incidents : incidents.filter((item) => item.status === alertFilter);
  const transitionAlert = (id, next) => { setIncidents((current) => current.map((item) => item.id === id ? { ...item, status: next } : item)); onAnnounce(next === "已关闭" ? "告警已关闭，处置记录已留痕" : "已确认告警并创建处置任务"); };
  const progressMaintenance = (id) => { setMaintenance((current) => current.map((item) => item.id === id ? { ...item, status: item.status === "待维护" ? "处理中" : "已完成" } : item)); onAnnounce("维护状态已更新并同步至设备档案"); };
  return <section className="device-center-workspace"><div className="module-hero"><div><p className="eyebrow"><span />设备资产与运行</p><h1>设备中心</h1><p>查看机构设备状态、使用情况、维护巡检与告警处置进度。</p></div><button type="button" className="button primary" onClick={() => onCreate("devices")}><Plus size={17} />登记设备</button></div><section className="device-kpis"><article><span>设备总数</span><strong>145</strong><small>房间与公共区域已纳管</small></article><article><span>设备在线率</span><strong>98.6%</strong><small>143 台运行正常</small></article><article><span>待维护</span><strong>{maintenance.filter((item) => item.status !== "已完成").length}</strong><small>含 1 项今日到期</small></article><article><span>待确认告警</span><strong>{incidents.filter((item) => item.status === "待确认").length}</strong><small>高优先级 15 分钟内响应</small></article></section><div className="device-tabs" role="tablist" aria-label="设备中心内容">{[["overview", "设备概览", Wifi], ["alerts", "告警处置", AlertTriangle], ["maintenance", "维护巡检", ClipboardCheck]].map(([id, label, Icon]) => <button type="button" role="tab" key={id} aria-selected={view === id} className={view === id ? "active" : ""} onClick={() => setView(id)}><Icon size={16} />{label}</button>)}</div>{view === "overview" && <section className="device-center-grid"><section className="panel device-inventory-panel"><div className="panel-heading"><div><h2>设备运行清单</h2><p>显示当前租户纳管设备的运行、使用与最近巡检信息</p></div><span className="detail-count">{visibleDevices.length} 台</span></div><div className="filter-row">{["全部", "在线", "低电量", "待检修"].map((item) => <button type="button" key={item} className={deviceFilter === item ? "filter active" : "filter"} onClick={() => setDeviceFilter(item)}>{item}</button>)}</div><div className="device-inventory-table"><div className="device-inventory-head"><span>设备</span><span>安装位置</span><span>运行状态</span><span>使用情况</span><span>最近巡检</span><span>维护</span></div>{visibleDevices.map((item) => <div className="device-inventory-row" key={item.id}><span><b>{item.name}</b><small>{item.code}</small></span><span>{item.location}</span><span className={item.status === "在线" ? "device-state online" : "device-state warning"}>{item.status}</span><span>{usage[item.id]}</span><span>{inspection[item.id]}</span><button type="button" className="text-action" onClick={() => onAnnounce(`${item.name} 的设备档案已打开`)}>查看</button></div>)}</div></section><aside className="panel device-status-panel"><div className="panel-heading compact"><div><h2>维护概览</h2><p>未来 7 天维护与巡检安排</p></div></div><div className="maintenance-mini-list">{maintenance.map((item) => <div key={item.id}><span className={item.status === "待维护" ? "maintenance-dot urgent" : "maintenance-dot"} /><p><b>{item.device}</b><small>{item.type} · {item.due}</small></p><span className="maintenance-status">{item.status}</span></div>)}</div><button type="button" className="full-text-button" onClick={() => setView("maintenance")}>查看维护计划 <ArrowRight size={16} /></button></aside></section>}{view === "alerts" && <section className="device-center-grid"><section className="panel incident-panel"><div className="panel-heading"><div><h2>实时告警队列</h2><p>高优先级告警按发生时间优先处理</p></div><span className="detail-count">{visibleAlerts.length} 条</span></div><div className="filter-row">{["全部", "待确认", "处理中", "已关闭"].map((item) => <button type="button" key={item} className={alertFilter === item ? "filter active" : "filter"} onClick={() => setAlertFilter(item)}>{item}</button>)}</div><div className="incident-list">{visibleAlerts.map((item) => <article key={item.id}><span className={`incident-dot ${item.level === "高优先级" ? "high" : item.level === "中优先级" ? "medium" : "low"}`} /><div className="incident-main"><b>{item.device}</b><small>{item.location} · {item.type} · {item.time}</small></div><span className="incident-level">{item.level}</span><StatusPill status={item.status} />{item.status === "待确认" ? <button type="button" className="button secondary incident-action" onClick={() => transitionAlert(item.id, "处理中")}>确认并派单</button> : item.status === "处理中" ? <button type="button" className="button primary incident-action" onClick={() => transitionAlert(item.id, "已关闭")}>关闭告警</button> : <span className="incident-closed"><Check size={16} />已留痕</span>}</article>)}</div></section><aside className="panel incident-aside"><div className="panel-heading compact"><div><h2>处置规则</h2><p>告警、工单与审计记录自动关联</p></div></div><div className="incident-rule-list"><p><AlertTriangle size={16} /><span><b>高优先级</b><small>通知值班护士与工程负责人，15 分钟内确认</small></span></p><p><Clock3 size={16} /><span><b>处理中</b><small>记录负责人、现场操作与预计恢复时间</small></span></p><p><ShieldCheck size={16} /><span><b>关闭留痕</b><small>需填写处置结果，保留到设备维护档案</small></span></p></div><button type="button" className="full-text-button" onClick={() => onAnnounce("设备巡检单已生成")}>生成今日巡检单 <ArrowRight size={16} /></button></aside></section>}{view === "maintenance" && <section className="device-center-grid"><section className="panel maintenance-panel"><div className="panel-heading"><div><h2>维护与巡检计划</h2><p>处理中的工单会同步关联至设备档案和告警处置</p></div><button type="button" className="text-action" onClick={() => onAnnounce("已导出本周维护计划")}>导出计划<ArrowRight size={15} /></button></div><div className="maintenance-list">{maintenance.map((item) => <article key={item.id}><span className={item.status === "待维护" ? "maintenance-icon urgent" : "maintenance-icon"}><Settings2 size={16} /></span><div><b>{item.device}</b><small>{item.location} · {item.type}</small></div><span className="maintenance-due">{item.due}</span><span className="maintenance-owner">{item.owner}</span><StatusPill status={item.status === "已完成" ? "已完成" : item.status === "处理中" ? "进行中" : "待确认"} />{item.status !== "已完成" && <button type="button" className="button secondary incident-action" onClick={() => progressMaintenance(item.id)}>{item.status === "待维护" ? "开始维护" : "完成维护"}</button>}</article>)}</div></section><aside className="panel device-status-panel"><div className="panel-heading compact"><div><h2>使用趋势</h2><p>当前机构设备使用摘要</p></div></div><div className="usage-summary"><div><span>紧急呼叫</span><b>12 次</b><small>今日测试与呼叫</small></div><div><span>睡眠监测</span><b>8 小时</b><small>昨夜有效数据</small></div><div><span>围栏通行</span><b>31 次</b><small>今日康复花园</small></div></div></aside></section>}</section>;
}

function DeviceIncidentWorkspace({ records, onCreate, onAnnounce }) {
  const [filter, setFilter] = useState("全部");
  const [incidents, setIncidents] = useState([{ id: 1, device: "紧急呼叫按钮", location: "C-403 卫生间", type: "低电量", level: "高优先级", status: "待确认", time: "09:12" }, { id: 2, device: "电子围栏", location: "康复花园", type: "通讯异常", level: "中优先级", status: "处理中", time: "08:45" }, { id: 3, device: "睡眠监测带", location: "B-208 房", type: "离线 8 分钟", level: "一般", status: "待确认", time: "08:26" }]);
  const visibleIncidents = filter === "全部" ? incidents : incidents.filter((item) => item.status === filter);
  const transition = (id, next) => {
    setIncidents((current) => current.map((item) => item.id === id ? { ...item, status: next } : item));
    onAnnounce(next === "已关闭" ? "告警已关闭，处置记录已留痕" : "已确认告警并创建处置任务");
  };
  return <section className="incident-workspace"><div className="module-hero"><div><p className="eyebrow"><span />设备告警闭环</p><h1>告警处置中心</h1><p>从设备异常到确认、维修和关闭，全程保留处理责任与时间线。</p></div><button type="button" className="button primary" onClick={() => onCreate("devices")}><Plus size={17} />登记设备</button></div><section className="dispatch-kpis"><article><span>待确认告警</span><strong>{incidents.filter((item) => item.status === "待确认").length}</strong><small>高优先级需 15 分钟内响应</small></article><article><span>处理中</span><strong>{incidents.filter((item) => item.status === "处理中").length}</strong><small>已指派工程与护理责任人</small></article><article><span>设备在线率</span><strong>98.6%</strong><small>{records.filter((item) => item.status === "在线").length} 台在线设备已纳入巡检</small></article></section><div className="incident-grid"><section className="panel incident-panel"><div className="panel-heading"><div><h2>实时告警队列</h2><p>高优先级告警按发生时间优先处理</p></div><span className="detail-count">{visibleIncidents.length} 条</span></div><div className="filter-row">{["全部", "待确认", "处理中", "已关闭"].map((item) => <button type="button" key={item} className={filter === item ? "filter active" : "filter"} onClick={() => setFilter(item)}>{item}</button>)}</div><div className="incident-list">{visibleIncidents.map((item) => <article key={item.id}><span className={`incident-dot ${item.level === "高优先级" ? "high" : item.level === "中优先级" ? "medium" : "low"}`} /><div className="incident-main"><b>{item.device}</b><small>{item.location} · {item.type} · {item.time}</small></div><span className="incident-level">{item.level}</span><StatusPill status={item.status} />{item.status === "待确认" ? <button type="button" className="button secondary incident-action" onClick={() => transition(item.id, "处理中")}>确认并派单</button> : item.status === "处理中" ? <button type="button" className="button primary incident-action" onClick={() => transition(item.id, "已关闭")}>关闭告警</button> : <span className="incident-closed"><Check size={16} />已留痕</span>}</article>)}</div></section><aside className="panel incident-aside"><div className="panel-heading compact"><div><h2>处置规则</h2><p>告警、工单与审计记录自动关联</p></div></div><div className="incident-rule-list"><p><AlertTriangle size={16} /><span><b>高优先级</b><small>通知值班护士与工程负责人，15 分钟内确认</small></span></p><p><Clock3 size={16} /><span><b>处理中</b><small>记录负责人、现场操作与预计恢复时间</small></span></p><p><ShieldCheck size={16} /><span><b>关闭留痕</b><small>需填写处置结果，保留到设备维护档案</small></span></p></div><button type="button" className="full-text-button" onClick={() => onAnnounce("设备巡检单已生成")}>生成今日巡检单 <ArrowRight size={16} /></button></aside></div></section>;
}

function ModuleWorkspace({ moduleId, records, onCreate, onEdit, onView, onDelete }) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("全部");
  const config = MODULE_CONFIG[moduleId];
  const visibleRecords = records.filter((record) => {
    const matchesQuery = Object.values(record).some((value) => String(value).toLowerCase().includes(query.toLowerCase()));
    return matchesQuery && (status === "全部" || record.status === status);
  });

  return (
    <section className="module-page">
      <div className="module-hero">
        <div><p className="eyebrow"><span />机构运营数据</p><h1>{config.title}</h1><p>{config.subtitle}</p></div>
        <button type="button" className="button primary" onClick={() => onCreate(moduleId)}><Plus size={17} />{config.create}</button>
      </div>
      <section className="panel module-panel">
        <div className="module-toolbar">
          <div className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={`搜索${config.title}内容`} aria-label={`搜索${config.title}`} /></div>
          <div className="status-filters" aria-label="状态筛选">{config.statuses.map((item) => <button type="button" className={status === item ? "filter active" : "filter"} key={item} onClick={() => setStatus(item)}>{item}</button>)}</div>
          <span className="result-count">共 {visibleRecords.length} 条</span>
        </div>
        <div className="module-table" style={{ "--columns": config.fields.length }}>
          <div className="module-table-head">{config.fields.map(([, label]) => <span key={label}>{label}</span>)}<span>操作</span></div>
          {visibleRecords.length ? visibleRecords.map((record) => <div className="module-table-row" key={record.id}>{config.fields.map(([key], index) => <span key={key} className={key === "status" ? "record-status" : index === 0 ? "record-main" : "record-value"}>{key === "status" ? <StatusPill status={record[key]} /> : record[key]}</span>)}<span className="row-actions"><button type="button" onClick={() => onView(moduleId, record)} aria-label={`查看${record.name}`}><Eye size={16} /></button><button type="button" onClick={() => onEdit(moduleId, record)} aria-label={`编辑${record.name}`}><Pencil size={15} /></button><button type="button" className="danger" onClick={() => onDelete(moduleId, record)} aria-label={`删除${record.name}`}><Trash2 size={15} /></button></span></div>) : <div className="table-empty"><Search size={20} /><p>没有找到匹配的示例记录</p><button type="button" onClick={() => { setQuery(""); setStatus("全部"); }}>清除筛选</button></div>}
        </div>
      </section>
    </section>
  );
}

function formatReportRate(value) {
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toFixed(1)}%` : "--";
}

function currentReportPeriod() {
  const today = new Date();
  const format = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  return { start: format(new Date(today.getFullYear(), today.getMonth(), 1)), end: format(today) };
}

function formatExportTime(value) {
  if (!value) return "--";
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? "--" : date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false });
}

function ReportWorkspace({ reportApi, onAnnounce, refreshKey }) {
  const [period, setPeriod] = useState(currentReportPeriod);
  const [report, setReport] = useState(DEMO_OPERATIONAL_REPORT);
  const [exportJobs, setExportJobs] = useState([]);
  const [demoExports, setDemoExports] = useState(DEMO_EXPORT_JOBS);
  const [working, setWorking] = useState(false);
  const [loadError, setLoadError] = useState("");

  const refresh = useCallback(async () => {
    if (!reportApi) return;
    setWorking(true);
    setLoadError("");
    try {
      const [nextReport, nextExports] = await Promise.all([
        reportApi.getOperationalReport({ periodStart: period.start, periodEnd: period.end }),
        reportApi.listExports(),
      ]);
      setReport(nextReport);
      setExportJobs(nextExports);
    } catch (error) {
      setLoadError(error.message || "运营报表加载失败");
    } finally {
      setWorking(false);
    }
  }, [period.end, period.start, reportApi]);

  useEffect(() => { void refresh(); }, [refresh, refreshKey]);

  const requestExport = async () => {
    if (!reportApi) {
      const now = new Date();
      setDemoExports((current) => [{
        id: `demo-export-${Date.now()}`,
        exportType: "OPERATIONAL_SUMMARY_CSV",
        periodStart: period.start,
        periodEnd: period.end,
        status: "READY",
        fileName: `机构运营汇总-${period.start}-${period.end}.csv`,
        createdAt: now.toISOString(),
        completedAt: now.toISOString(),
        expiresAt: new Date(now.valueOf() + 24 * 60 * 60 * 1000).toISOString(),
      }, ...current]);
      onAnnounce("运营汇总导出已生成，可在有效期内下载");
      return;
    }
    setWorking(true);
    try {
      await reportApi.createExport({ exportType: "OPERATIONAL_SUMMARY_CSV", periodStart: period.start, periodEnd: period.end });
      onAnnounce("导出任务已创建，文件生成后可下载");
      await refresh();
    } catch (error) {
      setLoadError(error.message || "创建导出任务失败");
    } finally {
      setWorking(false);
    }
  };

  const downloadExport = async (job) => {
    if (!reportApi) {
      onAnnounce(`${job.fileName} 已准备就绪（演示模式不会生成真实文件）`);
      return;
    }
    setWorking(true);
    try {
      const access = await reportApi.getExportAccess(job.id);
      window.open(access.accessUrl, "_blank", "noopener,noreferrer");
      onAnnounce("已签发短时下载地址，请在有效期内完成下载");
    } catch (error) {
      setLoadError(error.message || "下载授权失败");
      await refresh();
    } finally {
      setWorking(false);
    }
  };

  const jobs = reportApi ? exportJobs : demoExports;
  const metrics = [
    ["在院长者", report.residentsInResidence, `启用床位 ${report.enabledBeds || 0} 张`],
    ["床位使用率", formatReportRate(report.occupancyRate), `已占用 ${report.occupiedBeds || 0} 张`],
    ["护理任务完成率", formatReportRate(report.taskCompletionRate), `已完成 ${report.completedCareTasks || 0} / ${report.scheduledCareTasks || 0}`],
    ["开放跟进项", report.openFollowUps, `${report.exceptionCareTasks || 0} 项任务异常`],
  ];

  return <section className="report-workspace">
    <div className="module-hero"><div><p className="eyebrow"><span />机构运营汇总</p><h1>运营报表</h1><p>按当前机构与统计周期汇总入住、床位、护理执行和服务记录，并生成受控下载文件。</p></div><button type="button" className="button primary" disabled={working} onClick={() => void requestExport()}><Download size={17} />导出运营汇总</button></div>
    <section className="panel report-filter-panel"><div className="report-period-fields"><label>统计开始<input type="date" value={period.start} max={period.end} onChange={(event) => setPeriod((current) => ({ ...current, start: event.target.value }))} /></label><label>统计结束<input type="date" value={period.end} min={period.start} onChange={(event) => setPeriod((current) => ({ ...current, end: event.target.value }))} /></label><button type="button" className="button secondary" disabled={working} onClick={() => void refresh()}>刷新数据</button></div><p>导出仅包含机构级汇总指标，不包含长者身份、照护正文或附件内容。</p></section>
    {loadError && <section className="report-error"><AlertTriangle size={17} /><span>{loadError}</span><button type="button" onClick={() => void refresh()}>重新加载</button></section>}
    <section className="report-metrics" aria-label="运营汇总指标">{metrics.map(([label, value, note]) => <article key={label}><span>{label}</span><strong>{value ?? 0}</strong><small>{note}</small></article>)}</section>
    <section className="report-detail-grid"><section className="panel report-detail-panel"><div className="panel-heading"><div><h2>周期执行概览</h2><p>{period.start} 至 {period.end}</p></div><span className="detail-count">实时汇总</span></div><dl className="report-stat-list"><div><dt>入住申请</dt><dd>{report.admissionApplications || 0}</dd></div><div><dt>完成退住</dt><dd>{report.discharges || 0}</dd></div><div><dt>计划护理任务</dt><dd>{report.scheduledCareTasks || 0}</dd></div><div><dt>异常护理任务</dt><dd>{report.exceptionCareTasks || 0}</dd></div><div><dt>服务记录</dt><dd>{report.serviceRecords || 0}</dd></div><div><dt>开放跟进项</dt><dd>{report.openFollowUps || 0}</dd></div></dl></section><aside className="panel report-guide-panel"><div className="panel-heading compact"><div><h2>导出范围</h2><p>仅当前机构的授权数据</p></div></div><div className="report-guide-list"><p><ShieldCheck size={16} /><span><b>授权生成</b><small>创建与下载分别校验当前角色权限。</small></span></p><p><Clock3 size={16} /><span><b>短时可用</b><small>文件到期后不再签发下载地址。</small></span></p><p><ClipboardCheck size={16} /><span><b>最小化数据</b><small>CSV 仅输出运营聚合指标。</small></span></p></div></aside></section>
    <section className="panel export-center-panel"><div className="panel-heading"><div><h2>导出中心</h2><p>文件异步生成，状态变为“可下载”后才可获取短时访问地址。</p></div><span className="detail-count">{jobs.length} 个任务</span></div><div className="export-job-table"><div className="export-job-head"><span>导出文件</span><span>统计周期</span><span>创建时间</span><span>状态</span><span>操作</span></div>{jobs.length ? jobs.map((job) => <div className="export-job-row" key={job.id}><span><b>{job.fileName || "运营汇总 CSV"}</b><small>{job.exportType}</small></span><span>{job.periodStart} 至 {job.periodEnd}</span><span>{formatExportTime(job.createdAt)}</span><span><i className={`export-status ${String(job.status || "").toLowerCase()}`}>{EXPORT_STATUS_LABELS[job.status] || job.status}</i></span><span>{job.status === "READY" ? <button type="button" className="text-action" disabled={working} onClick={() => void downloadExport(job)}><Download size={15} />下载</button> : <small className="export-action-note">{job.status === "EXPIRED" ? "已失效" : job.status === "FAILED" ? "请重新创建" : "处理中"}</small>}</span></div>) : <div className="table-empty"><Download size={20} /><p>暂无导出任务</p></div>}</div></section>
  </section>;
}

function NotificationBell({ notificationApi, onAnnounce, refreshKey }) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([{ id: "demo-overdue", title: "护理任务已逾期", priority: "HIGH", category: "TASK_OVERDUE", createdAt: "2026-07-31T09:12:00+08:00", readAt: null }, { id: "demo-handover", title: "班次交接待接收", priority: "NORMAL", category: "HANDOVER", createdAt: "2026-07-31T08:45:00+08:00", readAt: null }]);
  const [working, setWorking] = useState(false);
  const refresh = useCallback(async () => {
    if (!notificationApi) return;
    try { setNotifications(await notificationApi.list()); } catch (error) { onAnnounce(error.message || "通知加载失败"); }
  }, [notificationApi, onAnnounce]);
  useEffect(() => { if (open) void refresh(); }, [open, refresh, refreshKey]);
  const markRead = async (item) => {
    if (item.readAt) return;
    if (!notificationApi) { setNotifications((current) => current.map((value) => value.id === item.id ? { ...value, readAt: new Date().toISOString() } : value)); return; }
    setWorking(true);
    try { await notificationApi.markRead(item.id); await refresh(); } finally { setWorking(false); }
  };
  const markAllRead = async () => {
    if (!notificationApi) { setNotifications((current) => current.map((value) => ({ ...value, readAt: value.readAt || new Date().toISOString() }))); return; }
    setWorking(true);
    try { await notificationApi.markAllRead(); await refresh(); } finally { setWorking(false); }
  };
  const unreadCount = notifications.filter((item) => !item.readAt).length;
  return <div className="notification-wrap"><button type="button" className="icon-button notification" aria-label="通知" aria-expanded={open} onClick={() => setOpen((current) => !current)}><Bell size={19} />{unreadCount > 0 && <i />}</button>{open && <section className="notification-popover"><header><div><b>通知中心</b><small>{unreadCount ? `${unreadCount} 条未读` : "全部已读"}</small></div>{unreadCount > 0 && <button type="button" disabled={working} onClick={() => void markAllRead()}>全部已读</button>}</header><div className="notification-list">{notifications.length ? notifications.map((item) => <button type="button" key={item.id} className={item.readAt ? "read" : "unread"} disabled={working} onClick={() => void markRead(item)}><span className={item.priority === "HIGH" ? "notification-dot high" : "notification-dot"} /><p><b>{item.title}</b><small>{item.category === "HANDOVER" ? "班次交接" : item.category === "ADMISSION" ? "入住管理" : "护理执行"} · {formatExportTime(item.createdAt)}</small></p></button>) : <p className="notification-empty">暂无通知</p>}</div></section>}</div>;
}

function RecordModal({ mode, moduleId, record, onClose, onSave }) {
  const config = MODULE_CONFIG[moduleId];
  const [form, setForm] = useState(() => Object.fromEntries(config.fields.map(([key]) => [key, record?.[key] ?? ""])));
  const isDetail = mode === "detail";
  const update = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}><form className="modal record-modal" onSubmit={(event) => { event.preventDefault(); onSave(form); }} onMouseDown={(event) => event.stopPropagation()}><div className="modal-title"><div><span className="modal-icon">{isDetail ? <Eye size={19} /> : mode === "edit" ? <Pencil size={18} /> : <Plus size={20} />}</span><div><h2>{isDetail ? `${config.title}详情` : mode === "edit" ? `编辑${config.title}` : config.create}</h2><p>{isDetail ? "查看当前记录的完整示例信息" : "填写后立即更新当前演示列表"}</p></div></div><button type="button" className="icon-button" onClick={onClose} aria-label="关闭"><X size={18} /></button></div><div className="record-fields">{config.fields.map(([key, label]) => <label key={key}>{label}{isDetail ? <output>{form[key] || "-"}</output> : key === "status" ? <select value={form[key]} onChange={(event) => update(key, event.target.value)}>{config.statuses.filter((item) => item !== "全部").map((item) => <option key={item}>{item}</option>)}</select> : <input required value={form[key]} onChange={(event) => update(key, event.target.value)} placeholder={`请输入${label}`} />}</label>)}</div><div className="modal-actions"><button type="button" className="button secondary" onClick={onClose}>{isDetail ? "关闭" : "取消"}</button>{!isDetail && <button type="submit" className="button primary">{mode === "edit" ? "保存修改" : "确认创建"}</button>}</div></form></div>;
}

function DeleteDialog({ record, moduleId, onClose, onConfirm }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}><section className="modal delete-modal" role="dialog" aria-modal="true" aria-labelledby="delete-title" onMouseDown={(event) => event.stopPropagation()}><span className="delete-icon"><Trash2 size={20} /></span><h2 id="delete-title">删除示例记录？</h2><p>将删除“{record.name}”及当前页面中的相关演示数据。此操作仅影响本次原型会话。</p><div className="modal-actions"><button type="button" className="button secondary" onClick={onClose}>取消</button><button type="button" className="button destructive" onClick={() => onConfirm(moduleId, record.id)}>确认删除</button></div></section></div>;
}

function CareAssistant({ onNavigate }) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([{ id: 1, role: "assistant", text: "你好，我是智护小助理。可以帮你查找系统操作路径，或解答日常照护与养老服务相关问题。" }]);
  const quickQuestions = ["如何为新入住长者分配床位？", "设备高优先级告警应怎样处理？", "跌倒风险长者需要关注什么？"];
  const answer = (question) => {
    if (question.includes("床位") || question.includes("入住")) return "可在“入住与床位”进入入住调度中心：先选择待评估长者，再从空闲床位中分配，最后由前台与护理共同确认入住。";
    if (question.includes("告警") || question.includes("设备")) return "进入“设备中心”的告警处置中心，优先确认高优先级告警并派单，记录现场处置后再关闭告警。紧急情况请按机构预案立即通知当班人员。";
    if (question.includes("跌倒") || question.includes("风险")) return "建议结合个人评估结果执行巡视、环境检查、助行与用药观察；出现急性变化或跌倒事件时，请按机构应急流程处置并及时评估。";
    if (question.includes("护理") || question.includes("计划")) return "可在“照护计划”查看当日执行队列。任务完成后会进入交接依据，异常情况应在服务记录中补充说明。";
    return "我可以协助定位系统功能，或提供一般养老服务知识参考。涉及诊断、用药、急救和个体化处置时，请以持证医护人员与机构规范为准。";
  };
  const ask = (question) => {
    const text = question.trim();
    if (!text) return;
    setMessages((current) => [...current, { id: Date.now(), role: "user", text }, { id: Date.now() + 1, role: "assistant", text: answer(text) }]);
    setInput("");
  };
  return <div className="care-assistant"><div className={open ? "assistant-panel open" : "assistant-panel"} aria-hidden={!open}><header className="assistant-panel-header"><div><span className="assistant-header-mark"><HeartPulse size={15} /></span><p><b>智护小助理</b><small>在线 · 系统与照护知识辅助</small></p></div><button type="button" className="icon-button" onClick={() => setOpen(false)} aria-label="关闭小助理"><X size={18} /></button></header><div className="assistant-intro"><div><p>hi，张院长</p><h2>有什么需要<br />我来协助？</h2><small>可询问系统操作、照护流程与养老服务知识。</small></div><img src="/care-assistant.png" alt="智护小助理机器人" /></div><div className="assistant-quick"><p>常用问题</p>{quickQuestions.map((question) => <button type="button" key={question} onClick={() => ask(question)}><span>{question}</span><ChevronRight size={16} /></button>)}</div><div className="assistant-shortcuts"><button type="button" onClick={() => { onNavigate("admission"); setOpen(false); }}>进入入住调度 <ArrowRight size={15} /></button><button type="button" onClick={() => { onNavigate("devices"); setOpen(false); }}>查看告警处置 <ArrowRight size={15} /></button></div><div className="assistant-conversation" aria-live="polite">{messages.slice(-4).map((message) => <p key={message.id} className={message.role === "user" ? "assistant-message user" : "assistant-message"}>{message.text}</p>)}</div><form className="assistant-input" onSubmit={(event) => { event.preventDefault(); ask(input); }}><textarea value={input} onChange={(event) => setInput(event.target.value)} placeholder="问问系统操作或养老照护问题..." aria-label="向智护小助理提问" /><button type="submit" className="icon-button" aria-label="发送问题"><ArrowRight size={18} /></button></form><p className="assistant-disclaimer">仅提供信息辅助，不替代医护判断、紧急处置或机构规范。</p></div><div className="assistant-fab-wrap"><span className={open ? "assistant-hint hidden" : "assistant-hint"}>遇到问题？问问我</span><button type="button" className="assistant-fab" aria-label="打开智护小助理" aria-expanded={open} onClick={() => setOpen((current) => !current)}><img src="/care-assistant.png" alt="" /></button></div></div>;
}

function TypewriterText({ text, animate, onProgress }) {
  const [shown, setShown] = useState(animate ? "" : text);
  useEffect(() => {
    if (!animate) { setShown(text); return undefined; }
    setShown("");
    let index = 0;
    const timer = window.setInterval(() => {
      index += 1;
      setShown(text.slice(0, index));
      onProgress?.();
      if (index >= text.length) window.clearInterval(timer);
    }, 15);
    return () => window.clearInterval(timer);
  }, [animate, text]);
  return <>{shown}{animate && shown.length < text.length && <i className="typewriter-cursor" />}</>;
}

function EnhancedCareAssistant({ onNavigate }) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [thinkingStep, setThinkingStep] = useState(-1);
  const [messages, setMessages] = useState([{ id: 1, role: "assistant", text: "你好，我是智护小助理。可以帮你查找系统操作路径，或解答日常照护与养老服务相关问题。", animate: false }]);
  const conversationRef = useRef(null);
  const quickQuestions = ["如何为新入住长者分配床位？", "设备高优先级告警应怎样处理？", "跌倒风险长者需要关注什么？"];
  const thinkingLabels = ["正在识别问题类型…", "正在检索系统流程与照护知识…", "正在整理可执行建议…"];
  const scrollToLatest = () => {
    window.requestAnimationFrame(() => {
      const conversation = conversationRef.current;
      if (conversation) conversation.scrollTop = conversation.scrollHeight;
    });
  };
  useEffect(() => {
    scrollToLatest();
  }, [messages, thinkingStep, open]);
  const answer = (question) => {
    if (question.includes("床位") || question.includes("入住")) return "可在“入住与床位”进入入住调度中心：先选择待评估长者，再从空闲床位中分配，最后由前台与护理共同确认入住。";
    if (question.includes("告警") || question.includes("设备")) return "可在“设备中心”的告警处置中确认高优先级告警并派单；设备概览可查看运行、使用和维护情况。紧急情况请按机构预案立即通知当班人员。";
    if (question.includes("跌倒") || question.includes("风险")) return "建议结合个人评估结果执行巡视、环境检查、助行与用药观察；出现急性变化或跌倒事件时，请按机构应急流程处置并及时评估。";
    if (question.includes("护理") || question.includes("计划")) return "可在“照护计划”查看当日执行队列。任务完成后会进入交接依据，异常情况应在服务记录中补充说明。";
    return "我可以协助定位系统功能，或提供一般养老服务知识参考。涉及诊断、用药、急救和个体化处置时，请以持证医护人员与机构规范为准。";
  };
  const ask = (question) => {
    const text = question.trim();
    if (!text || thinkingStep >= 0) return;
    setMessages((current) => [...current, { id: Date.now(), role: "user", text, animate: false }]);
    setInput("");
    setThinkingStep(0);
    window.setTimeout(() => setThinkingStep(1), 280);
    window.setTimeout(() => setThinkingStep(2), 620);
    window.setTimeout(() => { setThinkingStep(-1); setMessages((current) => [...current, { id: Date.now() + 1, role: "assistant", text: answer(text), animate: true }]); }, 980);
  };
  const panelClass = `assistant-panel${open ? " open" : ""}${messages.length > 1 || thinkingStep >= 0 ? " has-history" : ""}`;
  return <div className="care-assistant"><div className={panelClass} aria-hidden={!open}><header className="assistant-panel-header"><div><span className="assistant-header-mark"><HeartPulse size={15} /></span><p><b>智护小助理</b><small>在线 · 系统与照护知识辅助</small></p></div><button type="button" className="icon-button" onClick={() => setOpen(false)} aria-label="关闭小助理"><X size={18} /></button></header><div className="assistant-intro"><div><p>hi，张院长</p><h2>有什么需要<br />我来协助？</h2><small>可询问系统操作、照护流程与养老服务知识。</small></div><img src="/care-assistant.png" alt="智护小助理机器人" /></div><div className="assistant-quick"><p>常用问题</p>{quickQuestions.map((question) => <button type="button" key={question} disabled={thinkingStep >= 0} onClick={() => ask(question)}><span>{question}</span><ChevronRight size={16} /></button>)}</div><div className="assistant-shortcuts"><button type="button" onClick={() => { onNavigate("admission"); setOpen(false); }}>进入入住调度 <ArrowRight size={15} /></button><button type="button" onClick={() => { onNavigate("devices"); setOpen(false); }}>查看设备中心 <ArrowRight size={15} /></button></div><div ref={conversationRef} className="assistant-conversation" aria-live="polite">{messages.slice(-4).map((message) => <p key={message.id} className={message.role === "user" ? "assistant-message user" : "assistant-message"}><TypewriterText text={message.text} animate={message.animate} onProgress={scrollToLatest} /></p>)}{thinkingStep >= 0 && <p className="assistant-thinking"><Sparkles size={14} /><span>{thinkingLabels[thinkingStep]}</span><i /><i /><i /></p>}</div><form className="assistant-input" onSubmit={(event) => { event.preventDefault(); ask(input); }}><textarea value={input} disabled={thinkingStep >= 0} onChange={(event) => setInput(event.target.value)} placeholder={thinkingStep >= 0 ? "正在整理回答…" : "问问系统操作或养老照护问题..."} aria-label="向智护小助理提问" /><button type="submit" className="icon-button" disabled={thinkingStep >= 0} aria-label="发送问题"><ArrowRight size={18} /></button></form><p className="assistant-disclaimer">仅提供信息辅助，不替代医护判断、紧急处置或机构规范。</p></div><div className="assistant-fab-wrap"><span className={open ? "assistant-hint hidden" : "assistant-hint"}>遇到问题？问问我</span><button type="button" className="assistant-fab" aria-label="打开智护小助理" aria-expanded={open} onClick={() => setOpen((current) => !current)}><img src="/care-assistant.png" alt="" /></button></div></div>;
}

function SystemWorkspace({ tenant, tenants = TENANTS, systemApi, notificationApi, onBack, onTenantChange, onAnnounce, onTenantDirectoryChanged }) {
  const demoRoles = useMemo(() => [
    { roleId: "admin", roleName: "机构管理员", detail: "全机构管理、成员与权限配置", dataScope: "1" },
    { roleId: "care-lead", roleName: "护理主管", detail: "护理计划、评估与班次协同", dataScope: "3" },
    { roleId: "nurse", roleName: "责任护士", detail: "责任长者与执行任务", dataScope: "5" },
    { roleId: "social", roleName: "社工", detail: "家属沟通与服务记录", dataScope: "3" },
  ], []);
  const [section, setSection] = useState("members");
  const [members, setMembers] = useState(DEFAULT_MEMBERS);
  const [roles, setRoles] = useState(demoRoles);
  const [directoryUsers, setDirectoryUsers] = useState([]);
  const [query, setQuery] = useState("");
  const [showInvite, setShowInvite] = useState(false);
  const [invite, setInvite] = useState({ userId: "", roleId: "", name: "", email: "" });
  const [selectedRoleId, setSelectedRoleId] = useState(demoRoles[0].roleId);
  const [roleAccess, setRoleAccess] = useState({ checkedKeys: [], menus: [] });
  const [working, setWorking] = useState(false);
  const [loadError, setLoadError] = useState("");
  const selectedRole = roles.find((role) => String(role.roleId) === String(selectedRoleId)) || roles[0];
  const scopeLabels = { "1": "全部数据", "2": "自定义部门", "3": "本部门", "4": "本部门及下级", "5": "仅本人" };
  const toMember = (member) => ({
    id: member.userId || member.id,
    userId: member.userId || member.id,
    name: member.nickName || member.name || member.userName,
    email: member.email || "未填写邮箱",
    role: member.roleNames || member.role || "未配置角色",
    roleIds: member.roleIds || member.roleId || "",
    department: member.deptName || member.department || "未分配部门",
    scope: (member.dataScopes || member.scope || "5").split(",").map((item) => scopeLabels[item] || item).join(" / "),
    status: member.membershipStatus === "0" && member.userStatus !== "1" ? "已启用" : member.status || "已停用",
  });
  const refresh = useCallback(async () => {
    if (!systemApi) return;
    setWorking(true);
    setLoadError("");
    try {
      const [memberRows, roleRows, userRows] = await Promise.all([systemApi.listMembers(), systemApi.listRoles(), systemApi.listUsers()]);
      setMembers(memberRows.map(toMember));
      setRoles(roleRows.filter((role) => role.status === "0"));
      setDirectoryUsers(userRows.filter((user) => user.status === "0" && user.delFlag !== "2"));
    } catch (error) {
      setLoadError(error.message || "系统管理数据加载失败");
    } finally {
      setWorking(false);
    }
  }, [systemApi]);
  useEffect(() => { refresh(); }, [refresh, tenant.id]);
  useEffect(() => {
    if (roles.length && !roles.some((role) => String(role.roleId) === String(selectedRoleId))) setSelectedRoleId(roles[0].roleId);
  }, [roles, selectedRoleId]);
  useEffect(() => {
    if (!systemApi || !selectedRole?.roleId) return;
    systemApi.getRoleMenuAccess(selectedRole.roleId).then(setRoleAccess).catch(() => setRoleAccess({ checkedKeys: [], menus: [] }));
  }, [systemApi, selectedRole?.roleId]);
  const visibleMembers = members.filter((member) => `${member.name}${member.role}${member.department}`.toLowerCase().includes(query.toLowerCase()));
  const addMember = async (event) => {
    event.preventDefault();
    setWorking(true);
    try {
      if (systemApi) {
        await systemApi.addMember({ userId: Number(invite.userId), roleIds: [Number(invite.roleId)] });
        await refresh();
        onAnnounce("成员已加入当前机构，并已分配机构角色");
      } else {
        const role = roles.find((item) => String(item.roleId) === String(invite.roleId));
        setMembers((current) => [...current, { id: Date.now(), name: invite.name, email: invite.email, role: role?.roleName || "责任护士", department: "护理部", scope: "仅本人", status: "已启用" }]);
        onAnnounce("演示成员已加入当前机构");
      }
      setInvite({ userId: "", roleId: roles[0]?.roleId || "", name: "", email: "" });
      setShowInvite(false);
    } catch (error) {
      onAnnounce(error.message || "成员加入未完成");
    } finally {
      setWorking(false);
    }
  };
  const toggleMember = async (member) => {
    const nextStatus = member.status === "已停用" ? "已启用" : "已停用";
    setWorking(true);
    try {
      if (systemApi) {
        await systemApi.changeMemberStatus(member.userId, nextStatus === "已启用" ? "0" : "1");
        await refresh();
      } else setMembers((current) => current.map((item) => item.id === member.id ? { ...item, status: nextStatus } : item));
      onAnnounce(`成员已${nextStatus}`);
    } catch (error) {
      onAnnounce(error.message || "成员状态更新未完成");
    } finally {
      setWorking(false);
    }
  };
  const replaceMemberRoles = async (member, event) => {
    const roleIds = [...event.target.selectedOptions].map((option) => Number(option.value));
    if (!roleIds.length) return;
    setWorking(true);
    try {
      if (systemApi) {
        await systemApi.replaceMemberRoles(member.userId, roleIds);
        await refresh();
      } else {
        const selected = roles.filter((role) => roleIds.includes(Number(role.roleId)) || roleIds.includes(role.roleId));
        setMembers((current) => current.map((item) => item.id === member.id ? { ...item, role: selected.map((role) => role.roleName).join("、"), roleIds: roleIds.join(",") } : item));
      }
      onAnnounce("成员机构角色已更新");
    } catch (error) {
      onAnnounce(error.message || "成员角色更新未完成");
    } finally {
      setWorking(false);
    }
  };
  const saveDataScope = async () => {
    if (!selectedRole) return;
    setWorking(true);
    try {
      if (systemApi) await systemApi.updateRoleDataScope(selectedRole);
      onAnnounce("角色数据范围已保存");
    } catch (error) {
      onAnnounce(error.message || "角色数据范围保存未完成");
    } finally {
      setWorking(false);
    }
  };
  const updateScope = (dataScope) => setRoles((current) => current.map((role) => String(role.roleId) === String(selectedRoleId) ? { ...role, dataScope } : role));
  const memberCount = systemApi ? members.length : tenant.members;
  return <section className="system-workspace">
    <button type="button" className="back-link" onClick={onBack}><ArrowLeft size={16} />返回工作台</button>
    <div className="module-hero system-hero"><div><p className="eyebrow"><span />租户与访问控制</p><h1>系统管理</h1><p>{tenant.name} · 成员、角色和业务数据均绑定到当前机构租户</p></div><div className="system-hero-status"><ShieldCheck size={17} /><span>租户隔离已启用</span></div></div>
    <section className="tenant-context"><div><span className="tenant-icon"><Building2 size={18} /></span><p><small>当前租户</small><b>{tenant.name}</b><em>{tenant.code}</em></p></div><dl><div><dt>订阅计划</dt><dd>{tenant.plan}</dd></div><div><dt>机构成员</dt><dd>{memberCount} 人</dd></div><div><dt>在院长者</dt><dd>{tenant.residents} 人</dd></div><div><dt>数据区域</dt><dd>{tenant.region}</dd></div></dl></section>
    <div className="system-tabs" role="tablist" aria-label="系统管理内容">{[["members", "成员与角色", UsersRound], ["permissions", "权限策略", LockKeyhole], ["tenants", "租户与组织", Building2], ["audit", "统一审计", ClipboardCheck]].map(([id, label, Icon]) => <button key={id} type="button" role="tab" aria-selected={section === id} className={section === id ? "active" : ""} onClick={() => setSection(id)}><Icon size={16} />{label}</button>)}</div>
    {loadError && <p className="auth-feedback"><ShieldAlert size={16} />{loadError}</p>}
    {section === "members" && <section className="system-grid"><section className="panel system-main-panel"><div className="panel-heading"><div><h2>成员目录</h2><p>当前租户内的账号、角色、部门与数据范围</p></div><button type="button" className="button primary compact-button" onClick={() => setShowInvite(true)} disabled={working}><Plus size={16} />添加成员</button></div><div className="system-toolbar"><label className="search-field"><Search size={15} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索成员、角色或部门" aria-label="搜索成员" /></label><span>{working ? "正在同步" : `${visibleMembers.length} 位成员`}</span></div><div className="system-member-table"><div className="system-table-head"><span>成员</span><span>机构角色</span><span>部门</span><span>数据范围</span><span>状态</span><span>操作</span></div>{visibleMembers.map((member) => <div className="system-table-row" key={member.id}><span className="system-person"><i>{member.name.slice(0, 1)}</i><b>{member.name}<small>{member.email}</small></b></span><span><select multiple value={String(member.roleIds || "").split(",").filter(Boolean)} onChange={(event) => replaceMemberRoles(member, event)} aria-label={`${member.name}的机构角色`}>{roles.map((role) => <option key={role.roleId} value={role.roleId}>{role.roleName}</option>)}</select><small>{member.role}</small></span><span>{member.department}</span><span className="scope-tag">{member.scope}</span><span><StatusPill status={member.status === "已启用" ? "已完成" : "待处理"} /></span><button type="button" className="text-action" onClick={() => toggleMember(member)} disabled={working}>{member.status === "已停用" ? "启用" : "停用"}</button></div>)}</div></section><aside className="panel system-aside"><div className="panel-heading compact"><div><h2>角色概览</h2><p>{roles.length} 个启用角色</p></div></div>{roles.slice(0, 5).map((role) => <button type="button" className={String(selectedRoleId) === String(role.roleId) ? "role-row active" : "role-row"} key={role.roleId} onClick={() => { setSelectedRoleId(role.roleId); setSection("permissions"); }}><span><UserCog size={15} /></span><p><b>{role.roleName}</b><small>{role.detail || scopeLabels[role.dataScope] || "RuoYi 角色"}</small></p><ChevronRight size={16} /></button>)}</aside></section>}
    {section === "permissions" && <section className="system-grid"><section className="panel system-main-panel"><div className="panel-heading"><div><h2>权限策略</h2><p>角色菜单由 RuoYi 管理，数据范围应用到当前选中角色</p></div><span className="detail-count">{roleAccess.checkedKeys?.length || 0} 项菜单权限</span></div><div className="role-picker" role="listbox" aria-label="选择角色">{roles.map((role) => <button type="button" key={role.roleId} className={String(selectedRoleId) === String(role.roleId) ? "active" : ""} onClick={() => setSelectedRoleId(role.roleId)}>{role.roleName}</button>)}</div>{selectedRole && <div className="permission-list"><label><span><b>角色数据范围</b><small>由 RuoYi 数据权限规则在服务端限制记录可见范围</small></span><select value={selectedRole.dataScope || "5"} onChange={(event) => updateScope(event.target.value)}>{Object.entries(scopeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label><span><b>已授权菜单</b><small>{roleAccess.checkedKeys?.length || 0} 项菜单由角色菜单树授予；修改菜单权限需在 RuoYi 角色定义中维护</small></span><strong>{roleAccess.checkedKeys?.length || 0}</strong></label></div>}<div className="panel-footer"><span>当前角色：<b>{selectedRole?.roleName || "未选择"}</b></span><button type="button" className="button primary" onClick={saveDataScope} disabled={working || !selectedRole}>保存数据范围</button></div></section><aside className="panel system-aside access-aside"><div className="panel-heading compact"><div><h2>隔离规则</h2><p>成员目录请求由 Gateway 校验当前租户</p></div></div><div className="isolation-list"><p><ShieldCheck size={16} /><span><b>租户边界</b><small>成员只能访问已加入机构的数据</small></span></p><p><LockKeyhole size={16} /><span><b>角色授权</b><small>机构角色与 RuoYi 菜单权限共同生效</small></span></p><p><ClipboardCheck size={16} /><span><b>操作留痕</b><small>成员与角色变更写入 RuoYi 操作日志</small></span></p></div></aside></section>}
    {section === "tenants" && <section className="tenant-directory">{tenants.map((item) => <article className={item.id === tenant.id ? "tenant-card active" : "tenant-card"} key={item.id}><div className="tenant-card-head"><span className="tenant-icon"><Building2 size={18} /></span><span>{item.id === tenant.id && <em>当前租户</em>}<b>{item.name}</b><small>{item.code} · {item.region}</small></span></div><dl><div><dt>成员</dt><dd>{item.id === tenant.id ? memberCount : item.members} 人</dd></div><div><dt>长者</dt><dd>{item.residents} 人</dd></div><div><dt>版本</dt><dd>{item.plan}</dd></div></dl><button type="button" className={item.id === tenant.id ? "button secondary" : "button primary"} onClick={() => onTenantChange(item)}>{item.id === tenant.id ? "当前机构" : "切换到此机构"}</button></article>)}</section>}
    {section === "audit" && notificationApi && <AuditWorkspace notificationApi={notificationApi} onAnnounce={onAnnounce} />}
    <TenantCrudPanel visible={section === "tenants"} tenants={tenants} systemApi={systemApi} onAnnounce={onAnnounce} onRefresh={onTenantDirectoryChanged} />
    {showInvite && <div className="modal-backdrop" role="presentation" onMouseDown={() => setShowInvite(false)}><form className="modal record-modal" onSubmit={addMember} onMouseDown={(event) => event.stopPropagation()}><div className="modal-title"><div><span className="modal-icon"><UserRound size={19} /></span><div><h2>添加机构成员</h2><p>{systemApi ? `从 RuoYi 系统账号目录加入当前租户 ${tenant.name}` : `添加演示成员到 ${tenant.name}`}</p></div></div><button type="button" className="icon-button" onClick={() => setShowInvite(false)} aria-label="关闭"><X size={18} /></button></div><div className="record-fields">{systemApi ? <label>系统账号<select required value={invite.userId} onChange={(event) => setInvite({ ...invite, userId: event.target.value })}><option value="">请选择 RuoYi 系统账号</option>{directoryUsers.filter((user) => !members.some((member) => String(member.userId) === String(user.userId))).map((user) => <option key={user.userId} value={user.userId}>{user.nickName || user.userName} · {user.userName}</option>)}</select></label> : <><label>成员姓名<input required value={invite.name} onChange={(event) => setInvite({ ...invite, name: event.target.value })} placeholder="请输入姓名" /></label><label>工作邮箱<input required type="email" value={invite.email} onChange={(event) => setInvite({ ...invite, email: event.target.value })} placeholder="name@organization.care" /></label></>}<label>机构角色<select required value={invite.roleId} onChange={(event) => setInvite({ ...invite, roleId: event.target.value })}><option value="">请选择角色</option>{roles.map((role) => <option key={role.roleId} value={role.roleId}>{role.roleName}</option>)}</select></label></div><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setShowInvite(false)}>取消</button><button type="submit" className="button primary" disabled={working}>加入机构</button></div></form></div>}
  </section>;
}

function AccountPanel({ type, onClose, onSave, onLogout }) {
  const [profile, setProfile] = useState({ name: "张院长", phone: "138 0000 2186", email: "zhang@yiheyuan.care", role: "机构管理员" });
  const isProfile = type === "profile";
  const isSystem = type === "system";
  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}><section className="modal account-panel" role="dialog" aria-modal="true" aria-labelledby="account-title" onMouseDown={(event) => event.stopPropagation()}><div className="modal-title"><div><span className="modal-icon">{isProfile ? <UserRound size={19} /> : isSystem ? <UserCog size={19} /> : <LogOut size={19} />}</span><div><h2 id="account-title">{isProfile ? "个人中心" : isSystem ? "系统管理" : "退出登录"}</h2><p>{isProfile ? "维护您的账号信息与通知方式" : isSystem ? "管理机构成员、角色与访问权限" : "确认结束当前原型会话"}</p></div></div><button type="button" className="icon-button" onClick={onClose} aria-label="关闭"><X size={18} /></button></div>{isProfile ? <form onSubmit={(event) => { event.preventDefault(); onSave(); }}><div className="account-identity"><img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80" alt="张院长" /><div><b>{profile.name}</b><span>{profile.role}</span><small>颐和苑养老中心</small></div></div><div className="record-fields"><label>姓名<input value={profile.name} onChange={(event) => setProfile({ ...profile, name: event.target.value })} /></label><label>职务<input value={profile.role} onChange={(event) => setProfile({ ...profile, role: event.target.value })} /></label><label>联系电话<input value={profile.phone} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} /></label><label>邮箱地址<input value={profile.email} onChange={(event) => setProfile({ ...profile, email: event.target.value })} /></label></div><div className="account-note"><Mail size={16} /><span>每日运营摘要将发送至当前邮箱</span></div><div className="modal-actions"><button type="button" className="button secondary" onClick={onClose}>取消</button><button type="submit" className="button primary">保存资料</button></div></form> : isSystem ? <><div className="system-summary"><div><span>机构成员</span><strong>42</strong><small>含 18 名护理人员</small></div><div><span>启用角色</span><strong>5</strong><small>权限策略运行正常</small></div></div><div className="system-links"><button type="button" onClick={() => onSave("已打开成员管理示例") }><span><UserRound size={17} /><b>成员与角色</b><small>查看账号、角色及所属部门</small></span><ChevronRight size={18} /></button><button type="button" onClick={() => onSave("已打开访问权限示例") }><span><LockKeyhole size={17} /><b>访问权限</b><small>配置模块范围与数据访问级别</small></span><ChevronRight size={18} /></button></div><div className="modal-actions"><button type="button" className="button primary" onClick={onClose}>完成</button></div></> : <><div className="logout-message"><span><LogOut size={22} /></span><p>退出后将返回登录入口，当前页面内的示例数据会重置。</p></div><div className="modal-actions"><button type="button" className="button secondary" onClick={onClose}>取消</button><button type="button" className="button destructive" onClick={onLogout}>确认退出</button></div></>}</section></div>;
}

function AuthScreen({ onAuthenticated, notice }) {
  const [mode, setMode] = useState("login");
  const [feedback, setFeedback] = useState(notice);
  const [form, setForm] = useState({ account: "admin@yiheyuan.care", password: "123456", name: "", organization: "" });
  const update = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event) => {
    event.preventDefault();
    if (mode === "login") {
      try {
        await onAuthenticated({ account: form.account, password: form.password, demo: false });
      } catch (error) {
        setFeedback(error.message || "登录未完成，请稍后重试。");
      }
    }
    if (mode === "register") { setFeedback("注册信息已提交，请使用您的账号登录。"); setMode("login"); }
    if (mode === "forgot") setFeedback("重置指引已发送至您的登录邮箱。");
  };
  const titles = { login: ["欢迎回来", "登录后继续管理机构照护工作"], register: ["创建机构账号", "开始搭建您的智慧养老工作空间"], forgot: ["找回登录密码", "输入账号，我们会发送重置指引"] };
  return <main className="auth-page"><header className="auth-header"><a href="#login" className="brand"><span className="brand-mark"><HeartPulse size={20} /></span><span>智护云<span>Care</span></span></a><button type="button" className="auth-help" onClick={() => setFeedback("在线支持将在工作时间内响应您的问题。")}><Phone size={16} />联系支持</button></header><div className="auth-layout"><section className="auth-visual"><img src="https://images.unsplash.com/photo-1576765608866-5b51046452be?auto=format&fit=crop&w=1200&q=85" alt="护理人员陪伴长者" /><div className="auth-visual-copy"><span>SMART CARE, HUMAN TOUCH</span><h1>让每一次照护<br />都被安心看见</h1><p>服务于养老机构的协同照护与运营工作空间。</p><div><b>186</b><small>在院长长者 · 今日服务有序进行</small></div></div></section><section className="auth-form-side"><div className="auth-card"><div className="auth-card-head"><p className="eyebrow"><span />智慧养老 SaaS</p><h2>{titles[mode][0]}</h2><p>{titles[mode][1]}</p></div>{feedback && <div className="auth-feedback"><Check size={16} />{feedback}</div>}<form onSubmit={submit}>{mode === "register" && <><label>机构名称<input required value={form.organization} onChange={(event) => update("organization", event.target.value)} placeholder="例如：颐和苑养老中心" /></label><label>姓名<input required value={form.name} onChange={(event) => update("name", event.target.value)} placeholder="请输入您的姓名" /></label></>}<label>{mode === "register" ? "登录邮箱或手机" : "账号"}<input required value={form.account} onChange={(event) => update("account", event.target.value)} placeholder="邮箱或手机号" /></label>{mode !== "forgot" && <label>密码<div className="password-field"><input required type="password" value={form.password} onChange={(event) => update("password", event.target.value)} placeholder="请输入密码" /><LockKeyhole size={16} /></div></label>}{mode === "login" && <div className="auth-options"><label className="remember"><input type="checkbox" defaultChecked />记住登录状态</label><button type="button" onClick={() => { setFeedback(""); setMode("forgot"); }}>忘记密码？</button></div>}{mode === "register" && <label className="remember"><input required type="checkbox" />我已阅读并同意服务协议和隐私政策</label>}<button type="submit" className="auth-submit">{mode === "login" ? "登录工作台" : mode === "register" ? "创建账号" : "发送重置指引"}<ArrowRight size={17} /></button></form>{mode === "login" && <button type="button" className="demo-login" onClick={() => onAuthenticated({ demo: true })}><Sparkles size={16} />体验演示工作台</button>}<div className="auth-switch">{mode === "login" ? <>还没有机构账号？<button type="button" onClick={() => { setFeedback(""); setMode("register"); }}>立即注册</button></> : <>已有账号？<button type="button" onClick={() => { setFeedback(""); setMode("login"); }}>返回登录</button></>}</div></div></section></div></main>;
}

export function App() {
  const [activeNav, setActiveNav] = useState("overview");
  const [tasks, setTasks] = useState(INITIAL_TASKS);
  const [filter, setFilter] = useState("全部");
  const [mobileNav, setMobileNav] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [toast, setToast] = useState("");
  const [moduleRecords, setModuleRecords] = useState(INITIAL_MODULE_RECORDS);
  const [recordModal, setRecordModal] = useState(null);
  const [residentDetail, setResidentDetail] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [showAccountMenu, setShowAccountMenu] = useState(false);
  const [accountPanel, setAccountPanel] = useState(null);
  const [systemWorkspace, setSystemWorkspace] = useState(false);
  const [tenant, setTenant] = useState(TENANTS[0]);
  const [tenantOptions, setTenantOptions] = useState(TENANTS);
  const [showTenantMenu, setShowTenantMenu] = useState(false);
  const [authNotice, setAuthNotice] = useState("");
  const [authState, setAuthState] = useState({ status: "anonymous", profile: null });
  const accessTokenRef = useRef("");
  const tenantIdRef = useRef(TENANTS[0].id);
  const sessionStoreRef = useRef(createSessionStore());
  const authApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createAuthApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
  }) : null, []);
  const admissionApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createAdmissionApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const careApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createCareApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const residentApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createResidentApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const systemApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createSystemApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const exportApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createExportApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const masterDataApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createMasterDataApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const notificationApi = useMemo(() => runtimeConfig.authMode === "gateway" ? createNotificationApi({
    gatewayUrl: runtimeConfig.gatewayUrl,
    getAccessToken: () => accessTokenRef.current,
    getRequestHeaders: () => ({ "X-Tenant-Id": tenantIdRef.current }),
  }) : null, []);
  const isAuthenticated = authState.status === "authenticated";

  const filteredTasks = useMemo(
    () => filter === "全部" ? tasks : tasks.filter((task) => task.status === filter),
    [filter, tasks],
  );

  const navItem = NAV_ITEMS.find((item) => item.id === activeNav);

  const toUiTenant = (source) => ({
    ...TENANTS[0],
    id: String(source.id ?? source.tenantId),
    name: source.name || source.tenantName,
    code: source.code || source.tenantCode || TENANTS[0].code,
    plan: source.plan || TENANTS[0].plan,
    members: source.members ?? TENANTS[0].members,
    residents: source.residents ?? TENANTS[0].residents,
    region: source.region || TENANTS[0].region,
  });

  const authenticate = async ({ account, password, demo }) => {
    if (demo || runtimeConfig.authMode === "demo") {
      const session = sessionStoreRef.current.set({ mode: "demo", profile: { name: "张院长" }, tenant: TENANTS[0] });
      tenantIdRef.current = TENANTS[0].id;
      setTenant(TENANTS[0]);
      setTenantOptions(TENANTS);
      setAuthState({ status: "authenticated", profile: session.profile });
      setAuthNotice("");
      return;
    }

    setAuthState({ status: "checking", profile: null });
    try {
      const credentials = await authApi.login({ account, password });
      const accessToken = credentials.accessToken || credentials.token;
      if (!accessToken) throw new Error("认证服务未返回访问令牌");
      accessTokenRef.current = accessToken;
      const [profilePayload, availableTenants] = await Promise.all([authApi.getCurrentUser(), authApi.listTenants()]);
      const profile = profilePayload.user || profilePayload;
      const currentTenant = profile.currentTenant || availableTenants.find((item) => item.id === profile.tenantId) || availableTenants[0];
      if (!currentTenant) throw new Error("当前账户未分配可访问的机构租户");
      const session = sessionStoreRef.current.set({ mode: "gateway", credentials, profile, tenant: currentTenant });
      tenantIdRef.current = currentTenant.id;
      setTenant(toUiTenant(currentTenant));
      setTenantOptions(availableTenants.map(toUiTenant));
      setAuthState({ status: "authenticated", profile: session.profile });
      setAuthNotice("");
    } catch (error) {
      accessTokenRef.current = "";
      sessionStoreRef.current.clear();
      setAuthState({ status: "anonymous", profile: null });
      throw error;
    }
  };

  const changeTenant = async (nextTenant) => {
    if (nextTenant.id === tenant.id) {
      announce("当前已处于该租户");
      return;
    }
    try {
      if (runtimeConfig.authMode === "gateway") {
        const result = await authApi.switchTenant(nextTenant.id);
        if (result.accessToken || result.token) accessTokenRef.current = result.accessToken || result.token;
      }
      tenantIdRef.current = nextTenant.id;
      setTenant(nextTenant);
      announce(`已切换至${nextTenant.name}`);
    } catch (error) {
      announce(error.message || "切换租户未完成，请稍后重试");
    }
  };

  const refreshTenantDirectory = async () => {
    if (runtimeConfig.authMode !== "gateway" || !authApi) return;
    const nextTenants = await authApi.listTenants();
    const mapped = nextTenants.map(toUiTenant);
    setTenantOptions(mapped);
    if (!mapped.some((item) => item.id === tenant.id)) {
      setTenant(mapped[0] || TENANTS[0]);
    }
  };

  const signOut = () => {
    accessTokenRef.current = "";
    sessionStoreRef.current.clear();
    setAccountPanel(null);
    setTenantOptions(TENANTS);
    setAuthNotice("您已退出当前账户。");
    setAuthState({ status: "anonymous", profile: null });
  };

  const selectNav = (id) => {
    setActiveNav(id);
    if (id !== "residents") setResidentDetail(null);
    setSystemWorkspace(false);
    setMobileNav(false);
  };

  const completeTask = (id) => {
    setTasks((current) => current.map((task) => task.id === id ? { ...task, status: "已完成" } : task));
    setToast("任务已标记为完成，交接记录已同步更新");
    window.setTimeout(() => setToast(""), 3200);
  };

  const createRecord = (event) => {
    event.preventDefault();
    setShowModal(false);
    setToast("交接记录已创建，已通知当班护理组");
    window.setTimeout(() => setToast(""), 3200);
  };

  const announce = useCallback((message) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 3200);
  }, []);

  const openRecordModal = (mode, moduleId, record = null) => setRecordModal({ mode, moduleId, record });

  const openRecordView = (moduleId, record) => {
    if (moduleId === "residents") {
      setResidentDetail(record);
      return;
    }
    openRecordModal("detail", moduleId, record);
  };

  const saveModuleRecord = (form) => {
    const { mode, moduleId, record } = recordModal;
    setModuleRecords((current) => ({
      ...current,
      [moduleId]: mode === "create"
        ? [{ ...form, id: Date.now() }, ...current[moduleId]]
        : current[moduleId].map((item) => item.id === record.id ? { ...item, ...form } : item),
    }));
    setRecordModal(null);
    announce(mode === "create" ? "示例记录已创建" : "示例记录已保存");
  };

  const removeModuleRecord = (moduleId, id) => {
    setModuleRecords((current) => ({ ...current, [moduleId]: current[moduleId].filter((item) => item.id !== id) }));
    setDeleteTarget(null);
    announce("示例记录已删除");
  };

  const openAccountPanel = (type) => {
    setShowAccountMenu(false);
    if (type === "system") {
      setSystemWorkspace(true);
      return;
    }
    setAccountPanel(type);
  };

  const saveAccountPanel = (message = "个人资料已保存") => {
    setAccountPanel(null);
    announce(message);
  };

  if (!isAuthenticated) return <AuthScreen notice={authNotice} onAuthenticated={authenticate} />;

  return (
    <div className="app-shell">
      <header className="topbar">
        <a href="#overview" className="brand" onClick={() => selectNav("overview")}>
          <span className="brand-mark"><HeartPulse size={20} /></span>
          <span>智护云<span>Care</span></span>
        </a>

        <button className="mobile-menu" type="button" aria-label="打开导航" onClick={() => setMobileNav(!mobileNav)}>
          {mobileNav ? <X size={22} /> : <Menu size={22} />}
        </button>

        <nav className={mobileNav ? "top-nav is-open" : "top-nav"} aria-label="主导航">
          {NAV_ITEMS.map((item) => (
            <button key={item.id} type="button" className={activeNav === item.id ? "nav-link active" : "nav-link"} onClick={() => selectNav(item.id)}>
              {item.label}
            </button>
          ))}
        </nav>

        <div className="top-actions">
          <NotificationBell notificationApi={notificationApi} onAnnounce={announce} refreshKey={tenant.id} />
          <div className="tenant-menu-wrap">
            <button type="button" className="tenant-select" aria-label="切换机构租户" aria-expanded={showTenantMenu} onClick={() => setShowTenantMenu((current) => !current)}><Building2 size={17} />{tenant.name}<ChevronDown size={15} /></button>
            {showTenantMenu && <div className="tenant-menu" role="menu"><small>切换机构租户</small>{tenantOptions.map((item) => <button type="button" role="menuitem" key={item.id} className={item.id === tenant.id ? "active" : ""} onClick={() => { setShowTenantMenu(false); changeTenant(item); }}><span><b>{item.name}</b><em>{item.code}</em></span>{item.id === tenant.id && <Check size={16} />}</button>)}<p>切换后仅展示该机构授权范围内的数据</p></div>}
          </div>
          <div className="account-menu-wrap">
            <button type="button" className="profile" aria-label="打开账号菜单" aria-expanded={showAccountMenu} onClick={() => setShowAccountMenu((current) => !current)}><img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=80&q=80" alt="张院长" /></button>
            {showAccountMenu && <div className="account-menu" role="menu"><div className="account-menu-user"><img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=80&q=80" alt="" /><span><b>张院长</b><small>机构管理员</small></span></div><button type="button" role="menuitem" onClick={() => openAccountPanel("profile")}><UserRound size={16} />个人中心</button><button type="button" role="menuitem" onClick={() => openAccountPanel("system")}><UserCog size={16} />系统管理</button><div className="account-divider" /><button type="button" role="menuitem" className="menu-logout" onClick={() => openAccountPanel("logout")}><LogOut size={16} />退出登录</button></div>}
          </div>
        </div>
      </header>

      <main className="workspace">
        {systemWorkspace ? (
          <SystemWorkspace tenant={tenant} tenants={tenantOptions} systemApi={systemApi} notificationApi={notificationApi} onBack={() => setSystemWorkspace(false)} onTenantChange={changeTenant} onTenantDirectoryChanged={refreshTenantDirectory} onAnnounce={announce} />
        ) : residentDetail ? (
          <Resident360
            resident={residentDetail}
            tasks={tasks}
            plans={moduleRecords.care}
            onBack={() => setResidentDetail(null)}
            onOpenCare={() => { setResidentDetail(null); selectNav("care"); }}
            onCompleteTask={completeTask}
          />
        ) : activeNav === "overview" && runtimeConfig.authMode === "gateway" && admissionApi && careApi && residentApi && exportApi ? (
          <GatewayOperationalOverview admissionApi={admissionApi} careApi={careApi} residentApi={residentApi} reportApi={exportApi} onAnnounce={announce} />
        ) : activeNav === "overview" ? (
          <>
            <section className="page-intro">
              <div>
                <p className="eyebrow"><span />2026 年 7 月 29 日，星期三</p>
                <h1>早上好，张院长</h1>
                <p className="intro-copy">照护工作正按计划推进，今天有 <b>3 项</b> 需要优先关注的服务事项。</p>
              </div>
              <div className="intro-actions">
                <button className="button secondary" type="button"><CalendarDays size={17} />查看排班</button>
                <button className="button primary" type="button" onClick={() => setShowModal(true)}><FilePlus2 size={17} />创建交接记录</button>
              </div>
            </section>

            <section className="metrics" aria-label="关键运营指标">
              <MetricCard icon={UsersRound} label="在院长者" value="186" note="较上月 + 4 人" trend="up" accent="blue" />
              <MetricCard icon={BedDouble} label="床位使用率" value="84.5%" note="可用床位 34 张" accent="violet" />
              <MetricCard icon={ClipboardCheck} label="今日照护任务" value={`${tasks.filter((task) => task.status !== "已完成").length + 22}`} note="6 项待处理" accent="mint" />
              <MetricCard icon={AlertTriangle} label="待处理告警" value="3" note="含 1 项高优先级" accent="orange" />
            </section>

            <section className="content-grid">
              <div className="main-column">
                <section className="panel task-panel">
                  <div className="panel-heading">
                    <div><h2>今日照护任务</h2><p>按排班时间自动汇集，完成后同步进入交接记录</p></div>
                    <button type="button" className="text-action" onClick={() => selectNav("care")}>查看全部<ChevronRight size={16} /></button>
                  </div>
                  <div className="filter-row" role="tablist" aria-label="任务状态筛选">
                    {["全部", "待处理", "进行中", "待确认"].map((item) => <button type="button" key={item} onClick={() => setFilter(item)} className={filter === item ? "filter active" : "filter"}>{item}{item === "待处理" && <span>2</span>}</button>)}
                  </div>
                  <div className="task-table">
                    <div className="table-labels"><span>时间</span><span>服务对象</span><span>照护事项</span><span>负责人</span><span>状态</span><span /></div>
                    {filteredTasks.map((task) => (
                      <div className="task-row" key={task.id}>
                        <span className="task-time"><i className={task.level} />{task.time}</span>
                        <span className="resident-cell"><b>{task.name}</b><small>{task.room}</small></span>
                        <span className="task-name">{task.task}</span>
                        <span className="owner"><span>{task.owner.slice(0, 1)}</span>{task.owner}</span>
                        <StatusPill status={task.status} />
                        {task.status !== "已完成" ? <button type="button" className="finish-button" onClick={() => completeTask(task.id)} aria-label={`完成${task.task}`}><Check size={16} /></button> : <span className="finished"><Check size={15} /></span>}
                      </div>
                    ))}
                  </div>
                </section>

                <section className="panel care-focus">
                  <div className="panel-heading"><div><h2>护理重点追踪</h2><p>系统根据评估、任务和近期记录生成今日关注建议</p></div><button type="button" className="ai-label"><Sparkles size={15} />智能辅助</button></div>
                  <div className="focus-layout">
                    <div className="focus-person">
                      <img src="https://images.unsplash.com/photo-1488161628813-04466f872be2?auto=format&fit=crop&w=260&q=85" alt="王素兰" />
                      <div><p>重点关注长者</p><h3>王素兰 <span>82 岁</span></h3><small>B 区 208 房 · 入住第 126 天</small><button type="button" onClick={() => selectNav("residents")}>查看档案 <ArrowRight size={15} /></button></div>
                    </div>
                    <div className="focus-notes">
                      <div><span className="note-dot rose" /><p><b>跌倒风险</b><small>昨夜起夜 2 次，建议加强夜间巡视</small></p></div>
                      <div><span className="note-dot amber" /><p><b>营养观察</b><small>连续两日午餐摄入低于个人基线</small></p></div>
                      <div><span className="note-dot mint" /><p><b>家属沟通</b><small>女儿已预约今日 15:00 视频探视</small></p></div>
                    </div>
                  </div>
                </section>
              </div>

              <aside className="side-column">
                <section className="panel resident-panel">
                  <div className="panel-heading compact"><div><h2>需要关注</h2><p>长者状态提醒</p></div><button type="button" className="icon-button" aria-label="更多操作"><MoreHorizontal size={19} /></button></div>
                  <div className="resident-list">
                    {RESIDENTS.map((resident) => <article key={resident.name} className="resident-item"><img src={resident.image} alt="" /><div><b>{resident.name}</b><small>{resident.room}</small></div><span className={`resident-tag ${resident.tone}`}>{resident.tag}</span></article>)}
                  </div>
                  <button type="button" className="full-text-button" onClick={() => selectNav("residents")}>进入长者档案 <ArrowRight size={16} /></button>
                </section>

                <section className="panel device-panel">
                  <div className="panel-heading compact"><div><h2>设备状态</h2><p>房间与公共区域设备</p></div><span className="live-dot"><i />在线</span></div>
                  <div className="device-summary"><div><span>在线率</span><strong>98.6%</strong></div><div className="device-ring"><Wifi size={18} /><span>143 / 145</span></div></div>
                  <div className="alert-row"><span className="alert-icon"><AlertTriangle size={16} /></span><p><b>卫生间紧急按钮低电量</b><small>C-403 房 · 剩余 12%</small></p><ChevronRight size={17} /></div>
                  <button type="button" className="full-text-button" onClick={() => selectNav("devices")}>查看设备中心 <ArrowRight size={16} /></button>
                </section>

                <section className="security-note"><ShieldCheck size={18} /><p><b>数据已安全同步</b><span>最后更新于 09:18</span></p><button type="button" aria-label="设置"><Settings2 size={17} /></button></section>
              </aside>
            </section>
          </>
        ) : activeNav === "residents" && runtimeConfig.authMode === "gateway" && residentApi ? (
          <GatewayResidentWorkspace residentApi={residentApi} careApi={careApi} onAnnounce={announce} />
        ) : activeNav === "care" ? (
          runtimeConfig.authMode === "gateway" && careApi ? <GatewayCareWorkspace careApi={careApi} residentApi={residentApi} systemApi={systemApi} onAnnounce={announce} /> : <CareExecutionWorkspace
            tasks={tasks}
            plans={moduleRecords.care}
            onCompleteTask={completeTask}
            onCreate={openRecordModal}
            onEdit={(moduleId, record) => openRecordModal("edit", moduleId, record)}
            onView={openRecordView}
          />
        ) : activeNav === "admission" ? (
          runtimeConfig.authMode === "gateway" && admissionApi && residentApi ? <GatewayAdmissionPage admissionApi={admissionApi} residentApi={residentApi} onAnnounce={announce} /> : <AdmissionDispatchWorkspace records={moduleRecords.admission} onCreate={(moduleId) => openRecordModal("create", moduleId)} onAnnounce={announce} />
        ) : activeNav === "master-data" ? (
          runtimeConfig.authMode === "gateway" && masterDataApi ? <GatewayMasterDataWorkspace masterDataApi={masterDataApi} onAnnounce={announce} /> : <section className="module-page"><div className="module-hero"><div><p className="eyebrow"><span />机构基础资料</p><h1>基础资料管理</h1><p>请登录 Gateway 以维护真实房间、床位和照护目录。</p></div></div></section>
        ) : activeNav === "devices" ? (
          <DeviceCenterWorkspace records={moduleRecords.devices} onCreate={(moduleId) => openRecordModal("create", moduleId)} onAnnounce={announce} />
        ) : activeNav === "reports" ? (
          <ReportWorkspace reportApi={exportApi} onAnnounce={announce} refreshKey={tenant.id} />
        ) : (
          <ModuleWorkspace
            moduleId={activeNav}
            records={moduleRecords[activeNav]}
            onCreate={(moduleId) => openRecordModal("create", moduleId)}
            onEdit={(moduleId, record) => openRecordModal("edit", moduleId, record)}
            onView={openRecordView}
            onDelete={(moduleId, record) => setDeleteTarget({ moduleId, record })}
          />
        )}
      </main>

      {showModal && <div className="modal-backdrop" role="presentation" onMouseDown={() => setShowModal(false)}><form className="modal" onSubmit={createRecord} onMouseDown={(event) => event.stopPropagation()}><div className="modal-title"><div><span className="modal-icon"><FilePlus2 size={19} /></span><div><h2>创建交接记录</h2><p>将关键信息交给下一班护理人员</p></div></div><button type="button" className="icon-button" onClick={() => setShowModal(false)} aria-label="关闭"><X size={18} /></button></div><label>交接对象<select defaultValue="王素兰"><option>王素兰 · B-208</option><option>刘建国 · A-106</option><option>孙桂英 · C-403</option></select></label><label>交接内容<textarea required placeholder="记录当前状态、待办事项或特别注意事项" /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setShowModal(false)}>取消</button><button type="submit" className="button primary">创建并通知</button></div></form></div>}
      {recordModal && <RecordModal {...recordModal} onClose={() => setRecordModal(null)} onSave={saveModuleRecord} />}
      {deleteTarget && <DeleteDialog {...deleteTarget} onClose={() => setDeleteTarget(null)} onConfirm={removeModuleRecord} />}
      {accountPanel && <AccountPanel type={accountPanel} onClose={() => setAccountPanel(null)} onSave={saveAccountPanel} onLogout={signOut} />}
      <EnhancedCareAssistant onNavigate={selectNav} />
      {toast && <div className="toast"><Check size={17} />{toast}</div>}
    </div>
  );
}
