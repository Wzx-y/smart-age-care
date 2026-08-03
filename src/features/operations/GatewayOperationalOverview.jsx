import { useCallback, useEffect, useMemo, useState } from "react";
import { Activity, BedDouble, CalendarDays, ClipboardCheck, HeartPulse, RefreshCw, UsersRound } from "lucide-react";

const taskLabels = { PENDING: "待处理", IN_PROGRESS: "进行中", PENDING_CONFIRMATION: "待确认", COMPLETED: "已完成", EXCEPTION: "异常", OVERDUE: "已逾期", CANCELLED: "已取消" };

function firstDayOfMonth() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function GatewayOperationalOverview({ admissionApi, careApi, reportApi, residentApi, onAnnounce }) {
  const [data, setData] = useState({ report: null, tasks: [], residents: [], admissions: [] });
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [report, tasks, residents, admissions] = await Promise.all([
        reportApi.getOperationalReport({ periodStart: firstDayOfMonth(), periodEnd: today() }),
        careApi.listTasks(),
        residentApi.listResidents(),
        admissionApi.listAdmissions(),
      ]);
      setData({ report, tasks, residents, admissions });
    } catch (error) {
      onAnnounce(error.message || "机构总览加载失败");
    } finally {
      setLoading(false);
    }
  }, [admissionApi, careApi, onAnnounce, reportApi, residentApi]);

  useEffect(() => { void load(); }, [load]);

  const priorityTasks = useMemo(() => data.tasks.filter((task) => ["PENDING", "IN_PROGRESS", "OVERDUE", "EXCEPTION"].includes(task.status)).sort((a, b) => String(a.scheduledAt).localeCompare(String(b.scheduledAt))).slice(0, 6), [data.tasks]);
  const pendingAdmissions = useMemo(() => data.admissions.filter((item) => !["ADMITTED", "DISCHARGED", "CANCELLED"].includes(item.status)).slice(0, 5), [data.admissions]);
  const attentionResidents = useMemo(() => data.residents.filter((item) => item.status === "IN_RESIDENCE" || item.status === "PENDING_ADMISSION").slice(0, 6), [data.residents]);
  const report = data.report || {};

  return <section className="overview-page">
    <div className="page-heading"><div><p className="eyebrow"><span />当前机构运营</p><h1>今日照护决策</h1><p>数据来自当前机构的入住、床位、护理任务、长者档案与运营报表。</p></div><button type="button" className="button secondary" onClick={() => void load()} disabled={loading}><RefreshCw size={16} />{loading ? "刷新中" : "刷新数据"}</button></div>
    <section className="metrics-grid">
      <article className="metric-card"><div className="metric-icon violet"><UsersRound size={19} /></div><div className="metric-meta"><span>在院长者</span><strong>{report.residentsInResidence ?? "-"}</strong><small>本统计周期真实数据</small></div></article>
      <article className="metric-card"><div className="metric-icon mint"><BedDouble size={19} /></div><div className="metric-meta"><span>床位使用率</span><strong>{report.occupancyRate == null ? "-" : `${report.occupancyRate}%`}</strong><small>{report.occupiedBeds ?? "-"} / {report.enabledBeds ?? "-"} 张启用床位</small></div></article>
      <article className="metric-card"><div className="metric-icon amber"><ClipboardCheck size={19} /></div><div className="metric-meta"><span>任务完成率</span><strong>{report.taskCompletionRate == null ? "-" : `${report.taskCompletionRate}%`}</strong><small>{report.completedCareTasks ?? "-"} 项已完成</small></div></article>
      <article className="metric-card"><div className="metric-icon rose"><Activity size={19} /></div><div className="metric-meta"><span>开放跟进项</span><strong>{report.openFollowUps ?? "-"}</strong><small>{report.exceptionCareTasks ?? "-"} 项异常任务</small></div></article>
    </section>
    <div className="overview-grid">
      <section className="panel overview-primary"><div className="panel-heading"><div><h2>优先护理任务</h2><p>按计划时间展示待处理、执行中、逾期与异常任务</p></div><span className="detail-count">{priorityTasks.length} 项</span></div><div className="execution-list">{priorityTasks.length ? priorityTasks.map((task) => <article key={task.id}><div className="execution-time"><i className={task.status === "OVERDUE" || task.status === "EXCEPTION" ? "high" : "normal"} />{String(task.scheduledAt || "").slice(11, 16) || "--:--"}</div><div className="execution-task"><b>{task.taskName}</b><span>长者 #{task.residentId} · 成员 #{task.assigneeId}</span></div><span className="record-status">{taskLabels[task.status] || task.status}</span></article>) : <p className="empty-inline">当前没有需要处理的护理任务</p>}</div></section>
      <aside className="panel overview-side"><div className="panel-heading compact"><div><h2>入住调度</h2><p>待评估、待安排与待确认的申请</p></div><CalendarDays size={18} /></div><div className="execution-list">{pendingAdmissions.length ? pendingAdmissions.map((admission) => <article key={admission.id}><div className="execution-task"><b>{admission.residentName || `长者 #${admission.residentId}`}</b><span>{admission.roomNo || "尚未分配床位"}</span></div><span className="record-status">{admission.status}</span></article>) : <p className="empty-inline">没有待处理入住申请</p>}</div></aside>
    </div>
    <section className="panel overview-residents"><div className="panel-heading"><div><h2>长者关注</h2><p>当前机构在院或待入住的长者档案</p></div><HeartPulse size={19} /></div><div className="resident-cards">{attentionResidents.length ? attentionResidents.map((resident) => <article key={resident.id}><div className="resident-card-avatar"><UsersRound size={18} /></div><div><b>{resident.name}</b><span>{resident.status} · {resident.emergencyContactName || "未登记联系人"}</span></div></article>) : <p className="empty-inline">暂无可展示的长者档案</p>}</div></section>
  </section>;
}
