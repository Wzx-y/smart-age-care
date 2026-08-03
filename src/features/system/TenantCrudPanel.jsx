import { useState } from "react";
import { Building2, Pencil, Plus, Power, X } from "lucide-react";

const empty = { code: "", name: "", plan: "STANDARD", region: "" };

export function TenantCrudPanel({ visible, tenants, systemApi, onAnnounce, onRefresh }) {
  const [editing, setEditing] = useState(null);
  const [value, setValue] = useState(empty);
  const [working, setWorking] = useState(false);
  if (!visible || !systemApi) return null;
  const save = async (event) => {
    event.preventDefault(); setWorking(true);
    try {
      if (editing?.tenantId) await systemApi.updateTenant(editing.tenantId, { version: editing.version, details: value });
      else await systemApi.createTenant(value);
      setEditing(null); await onRefresh?.(); onAnnounce("机构资料已保存");
    } catch (error) { onAnnounce(error.message || "机构资料未保存"); }
    finally { setWorking(false); }
  };
  const startEdit = (tenant) => { setEditing(tenant); setValue({ code: tenant.code, name: tenant.name, plan: tenant.plan || "STANDARD", region: tenant.region || "" }); };
  return <section className="panel tenant-crud-panel"><div className="panel-heading"><div><h2>机构目录</h2><p>机构名称、编码、版本和启停由 RuoYi 系统服务维护。</p></div><button type="button" className="button primary" onClick={() => { setEditing({}); setValue(empty); }}><Plus size={16} />新建机构</button></div>{editing && <form className="record-fields" onSubmit={save}><label>机构编码<input required value={value.code} disabled={Boolean(editing.tenantId)} onChange={(event) => setValue({ ...value, code: event.target.value })} /></label><label>机构名称<input required value={value.name} onChange={(event) => setValue({ ...value, name: event.target.value })} /></label><label>订阅计划<select value={value.plan} onChange={(event) => setValue({ ...value, plan: event.target.value })}><option value="STANDARD">标准版</option><option value="PROFESSIONAL">专业版</option></select></label><label>区域<input value={value.region} onChange={(event) => setValue({ ...value, region: event.target.value })} /></label><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setEditing(null)}><X size={15} />取消</button><button type="submit" className="button primary" disabled={working}>保存</button></div></form>}<div className="tenant-directory tenant-directory-real">{tenants.map((tenant) => <article className="tenant-card" key={tenant.id || tenant.tenantId}><div className="tenant-card-head"><span className="tenant-icon"><Building2 size={18} /></span><span><b>{tenant.name}</b><small>{tenant.code} · {tenant.region || "未填写区域"}</small></span></div><dl><div><dt>成员</dt><dd>{tenant.members ?? "-"} 人</dd></div><div><dt>版本</dt><dd>{tenant.plan || "-"}</dd></div><div><dt>状态</dt><dd>{tenant.status === "1" ? "已停用" : "已启用"}</dd></div></dl><div className="row-actions"><button type="button" title="编辑机构" onClick={() => startEdit(tenant)}><Pencil size={15} /></button><button type="button" title="停用机构" onClick={() => void systemApi.disableTenant(tenant.id || tenant.tenantId, tenant.version).then(async () => { await onRefresh?.(); onAnnounce("机构已停用"); }).catch((error) => onAnnounce(error.message || "机构停用失败"))}><Power size={15} /></button></div></article>)}</div></section>;
}
