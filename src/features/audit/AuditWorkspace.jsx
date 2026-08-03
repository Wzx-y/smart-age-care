import { useCallback, useEffect, useState } from "react";
import { ClipboardCheck, Download, Filter, RefreshCw, Search } from "lucide-react";

export function AuditWorkspace({ notificationApi, onAnnounce }) {
  const [filters, setFilters] = useState({ resourceType: "", action: "", actorId: "", limit: "50" });
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const load = useCallback(async () => {
    setLoading(true);
    try { setEvents(await notificationApi.listAuditEvents(filters)); }
    catch (error) { onAnnounce(error.message || "审计记录加载失败"); }
    finally { setLoading(false); }
  }, [filters, notificationApi, onAnnounce]);
  useEffect(() => { void load(); }, [load]);
  const update = (key, value) => setFilters((current) => ({ ...current, [key]: value }));
  const exportCsv = () => {
    const header = "时间,资源类型,动作,资源ID,操作者ID,关联长者ID\n";
    const body = events.map((event) => [event.createdAt, event.resourceType, event.action, event.resourceId, event.actorId, event.relatedResidentId].map((value) => `"${String(value ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
    const url = URL.createObjectURL(new Blob(["\ufeff", header + body], { type: "text/csv;charset=utf-8" }));
    const anchor = document.createElement("a"); anchor.href = url; anchor.download = `审计记录-${new Date().toISOString().slice(0, 10)}.csv`; anchor.click(); URL.revokeObjectURL(url);
  };
  return <section className="system-grid audit-workspace"><section className="panel system-main-panel"><div className="panel-heading"><div><h2>统一审计查询</h2><p>只展示当前机构的动作、资源和时间，不展示档案或照护正文。</p></div><div><button type="button" className="text-action" onClick={() => void load()}><RefreshCw size={15} />刷新</button><button type="button" className="text-action" onClick={exportCsv} disabled={!events.length}><Download size={15} />导出当前结果</button></div></div><div className="record-fields compact-fields"><label>资源类型<input value={filters.resourceType} onChange={(event) => update("resourceType", event.target.value)} placeholder="如 ADMISSION" /></label><label>动作<input value={filters.action} onChange={(event) => update("action", event.target.value)} placeholder="如 RESIDENT_UPDATED" /></label><label>操作者 ID<input inputMode="numeric" value={filters.actorId} onChange={(event) => update("actorId", event.target.value)} /></label><label>条数<select value={filters.limit} onChange={(event) => update("limit", event.target.value)}><option value="50">50</option><option value="100">100</option><option value="255">255</option></select></label><button type="button" className="button secondary" onClick={() => void load()}><Search size={15} />查询</button></div><div className="module-table audit-table" style={{ "--columns": 5 }}>{loading ? <p className="table-empty">正在读取审计记录...</p> : <>{events.length ? events.map((event) => <div className="module-table-row" key={`${event.resourceType}-${event.resourceId}-${event.createdAt}-${event.action}`}><span className="record-main">{event.resourceType || "-"}</span><span className="record-value">{event.action}</span><span className="record-value">#{event.resourceId ?? "-"}</span><span className="record-value">成员 #{event.actorId ?? "系统"}</span><span className="record-value">{String(event.createdAt || "").replace("T", " ").slice(0, 19)}</span></div>) : <p className="table-empty"><ClipboardCheck size={20} />暂无审计记录</p>}</>}</div></section><aside className="panel system-aside"><div className="panel-heading compact"><div><h2>保留策略</h2><p>策略由服务端环境配置控制</p></div><Filter size={17} /></div><div className="isolation-list"><p><span><b>默认保留期</b><small>2555 天，按机构合规配置执行</small></span></p><p><span><b>清理方式</b><small>仅服务端定时任务清理过期记录</small></span></p><p><span><b>数据边界</b><small>跨机构资源不枚举，查询按当前租户过滤</small></span></p></div></aside></section>;
}
