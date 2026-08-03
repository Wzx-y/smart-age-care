import { useCallback, useEffect, useState } from "react";
import { Archive, ClipboardCheck, Download, Eye, FileText, HeartPulse, Pencil, Plus, Search, Trash2, UsersRound, X } from "lucide-react";

const STATUS_LABELS = {
  PENDING_ADMISSION: "待入住",
  IN_RESIDENCE: "在院",
  AWAY: "外出",
  DISCHARGED: "已退住",
  DECEASED: "已结案",
};

const RISK_LABELS = { LOW: "低风险", MEDIUM: "中风险", HIGH: "高风险" };

function ageFromBirthDate(value) {
  if (!value) return "-";
  const birthDate = new Date(value);
  if (Number.isNaN(birthDate.valueOf())) return "-";
  const today = new Date();
  const beforeBirthday = today.getMonth() < birthDate.getMonth()
    || (today.getMonth() === birthDate.getMonth() && today.getDate() < birthDate.getDate());
  return String(today.getFullYear() - birthDate.getFullYear() - (beforeBirthday ? 1 : 0));
}

function emptyResidentForm() {
  return { name: "", gender: "", birthDate: "", emergencyContactName: "", emergencyContactPhone: "" };
}

function residentFormFrom(record) {
  return {
    name: record.name || "",
    gender: record.gender || "",
    birthDate: record.birthDate || "",
    emergencyContactName: record.emergencyContactName || "",
    emergencyContactPhone: record.emergencyContactPhone || "",
  };
}

function emptyHealthProfile() {
  return {
    bloodType: "", allergySummary: "", chronicConditions: "", medicationNotes: "", careLevel: "",
    mobilityStatus: "", cognitionStatus: "", nutritionRisk: "", fallRisk: "", pressureInjuryRisk: "",
    infectionRisk: "", careNotes: "",
  };
}

function healthProfileFrom(profile) {
  return Object.fromEntries(Object.keys(emptyHealthProfile()).map((key) => [key, profile?.[key] || ""]));
}

function ResidentFields({ value, onChange }) {
  const fields = [
    ["name", "长者姓名", "text"],
    ["gender", "性别", "text"],
    ["birthDate", "出生日期", "date"],
    ["emergencyContactName", "紧急联系人", "text"],
    ["emergencyContactPhone", "紧急联系电话", "tel"],
  ];
  return <div className="record-fields">{fields.map(([key, label, type]) => <label key={key}>
    {label}
    <input required={key === "name" || key === "emergencyContactName"} type={type} value={value[key]} onChange={(event) => onChange((current) => ({ ...current, [key]: event.target.value }))} />
  </label>)}</div>;
}

function HealthFields({ value, onChange }) {
  const textFields = [["bloodType", "血型"], ["careLevel", "照护等级"], ["mobilityStatus", "行动能力"], ["cognitionStatus", "认知状态"]];
  const risks = [["nutritionRisk", "营养风险"], ["fallRisk", "跌倒风险"], ["pressureInjuryRisk", "压疮风险"], ["infectionRisk", "感染风险"]];
  return <div className="health-fields">
    <div className="record-fields">{textFields.map(([key, label]) => <label key={key}>{label}<input value={value[key]} onChange={(event) => onChange((current) => ({ ...current, [key]: event.target.value }))} /></label>)}</div>
    <div className="record-fields">{risks.map(([key, label]) => <label key={key}>{label}<select value={value[key]} onChange={(event) => onChange((current) => ({ ...current, [key]: event.target.value }))}><option value="">未评定</option><option value="LOW">低风险</option><option value="MEDIUM">中风险</option><option value="HIGH">高风险</option></select></label>)}</div>
    {[['allergySummary', '过敏史'], ['chronicConditions', '慢病情况'], ['medicationNotes', '用药说明'], ['careNotes', '照护注意事项']].map(([key, label]) => <label key={key}>{label}<textarea value={value[key]} onChange={(event) => onChange((current) => ({ ...current, [key]: event.target.value }))} /></label>)}
  </div>;
}

function contactFormFrom(contact) {
  return { name: contact.name || "", relationshipText: contact.relationshipText || "", phone: contact.phone || "", primaryContact: Boolean(contact.primaryContact) };
}

function assessmentFormFrom(assessment) {
  return { assessmentType: assessment.assessmentType || "ADL", assessmentDate: assessment.assessmentDate || new Date().toISOString().slice(0, 10), score: assessment.score ?? "", riskLevel: assessment.riskLevel || "MEDIUM", note: assessment.note || "" };
}

export function GatewayResidentWorkspace({ residentApi, careApi, onAnnounce }) {
  const [records, setRecords] = useState([]);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(null);
  const [profile, setProfile] = useState({ contacts: [], assessments: [], attachments: [], admissions: [], carePlans: [], careTasks: [], serviceRecords: [] });
  const [healthProfile, setHealthProfile] = useState(emptyHealthProfile);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editingHealth, setEditingHealth] = useState(false);
  const [editingContactId, setEditingContactId] = useState(null);
  const [editingAssessmentId, setEditingAssessmentId] = useState(null);
  const [showArchiveConfirmation, setShowArchiveConfirmation] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [residentForm, setResidentForm] = useState(emptyResidentForm);
  const [contactForm, setContactForm] = useState({ name: "", relationshipText: "", phone: "", primaryContact: false });
  const [assessmentForm, setAssessmentForm] = useState({ assessmentType: "ADL", assessmentDate: new Date().toISOString().slice(0, 10), score: "", riskLevel: "MEDIUM", note: "" });
  const [attachmentFile, setAttachmentFile] = useState(null);
  const [attachmentInputKey, setAttachmentInputKey] = useState(0);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);
  const [assessmentForPlan, setAssessmentForPlan] = useState(null);
  const [relatedPlanForm, setRelatedPlanForm] = useState({ planName: "", frequencyText: "每日", startDate: new Date().toISOString().slice(0, 10), endDate: "" });

  const refresh = useCallback(async (keyword = "") => {
    setLoading(true);
    try {
      setRecords(await residentApi.listResidents(keyword));
    } catch (error) {
      onAnnounce(error.message || "长者档案读取未完成");
    } finally {
      setLoading(false);
    }
  }, [onAnnounce, residentApi]);

  const loadProfile = useCallback(async (resident) => {
    setSelected(resident);
    setEditing(false);
    setEditingHealth(false);
    setEditingContactId(null);
    setEditingAssessmentId(null);
    setPendingDelete(null);
    setShowArchiveConfirmation(false);
    setAssessmentForPlan(null);
    try {
      const [detail, health, overview] = await Promise.all([
        residentApi.getResident(resident.id), residentApi.getHealthProfile(resident.id), residentApi.getResident360(resident.id),
      ]);
      setSelected(detail);
      setHealthProfile(healthProfileFrom(health));
      setProfile({
        contacts: overview.contacts || [], assessments: overview.assessments || [], attachments: overview.attachments || [],
        admissions: overview.admissions || [], carePlans: overview.carePlans || [], careTasks: overview.careTasks || [], serviceRecords: overview.serviceRecords || [],
      });
    } catch (error) {
      onAnnounce(error.message || "长者详情读取未完成");
    }
  }, [onAnnounce, residentApi]);

  useEffect(() => { void refresh(""); }, [refresh]);

  const reloadSelected = async () => { if (selected) await loadProfile(selected); };

  const submitResident = async (event) => {
    event.preventDefault();
    try {
      const created = await residentApi.createResident(residentForm);
      setResidentForm(emptyResidentForm()); setShowCreate(false);
      await refresh(""); await loadProfile(created);
      onAnnounce("长者档案已创建");
    } catch (error) { onAnnounce(error.message || "长者档案未创建"); }
  };

  const submitUpdate = async (event) => {
    event.preventDefault();
    if (!selected) return;
    try {
      const updated = await residentApi.updateResident(selected.id, residentForm);
      setEditing(false); await refresh(query); await loadProfile(updated);
      onAnnounce("长者档案已更新");
    } catch (error) { onAnnounce(error.message || "长者档案未更新"); }
  };

  const submitHealth = async (event) => {
    event.preventDefault();
    if (!selected) return;
    try {
      await residentApi.updateHealthProfile(selected.id, healthProfile);
      setEditingHealth(false); await reloadSelected();
      onAnnounce("健康档案已更新");
    } catch (error) { onAnnounce(error.message || "健康档案未更新"); }
  };

  const archiveResident = async () => {
    if (!selected) return;
    try {
      await residentApi.archiveResident(selected.id);
      setSelected(null); setProfile({ contacts: [], assessments: [], attachments: [], admissions: [], carePlans: [], careTasks: [], serviceRecords: [] });
      setShowArchiveConfirmation(false); await refresh(query);
      onAnnounce("长者档案已归档");
    } catch (error) { onAnnounce(error.message || "长者档案未归档"); }
  };

  const submitContact = async (event) => {
    event.preventDefault();
    if (!selected) return;
    try {
      const current = profile.contacts.find((item) => item.id === editingContactId);
      const payload = { ...contactForm, phone: contactForm.phone || null };
      if (current) await residentApi.updateContact(selected.id, current.id, { ...payload, version: current.version });
      else await residentApi.createContact(selected.id, payload);
      setContactForm({ name: "", relationshipText: "", phone: "", primaryContact: false }); setEditingContactId(null);
      await reloadSelected(); onAnnounce(current ? "联系人已更新" : "联系人已保存");
    } catch (error) { onAnnounce(error.status === 409 ? "联系人已变化，已刷新最新资料" : (error.message || "联系人未保存")); if (error.status === 409) await reloadSelected(); }
  };

  const submitAssessment = async (event) => {
    event.preventDefault();
    if (!selected) return;
    const payload = { ...assessmentForm, score: assessmentForm.score === "" ? null : Number(assessmentForm.score) };
    try {
      const current = profile.assessments.find((item) => item.id === editingAssessmentId);
      if (current) await residentApi.updateAssessment(selected.id, current.id, { ...payload, version: current.version });
      else await residentApi.createAssessment(selected.id, payload);
      setAssessmentForm((value) => ({ ...value, score: "", note: "" })); setEditingAssessmentId(null);
      await reloadSelected(); onAnnounce(current ? "评估记录已更新" : "评估记录已保存");
    } catch (error) { onAnnounce(error.status === 409 ? "评估记录已变化，已刷新最新资料" : (error.message || "评估记录未保存")); if (error.status === 409) await reloadSelected(); }
  };

  const confirmDelete = async () => {
    if (!selected || !pendingDelete) return;
    try {
      if (pendingDelete.type === "contact") await residentApi.deleteContact(selected.id, pendingDelete.record.id, pendingDelete.record.version);
      else await residentApi.deleteAssessment(selected.id, pendingDelete.record.id, pendingDelete.record.version);
      setPendingDelete(null); await reloadSelected(); onAnnounce(pendingDelete.type === "contact" ? "联系人已删除" : "评估记录已删除");
    } catch (error) { onAnnounce(error.status === 409 ? "记录已变化，已刷新最新资料" : (error.message || "删除未完成")); if (error.status === 409) await reloadSelected(); }
  };

  const submitAttachment = async (event) => {
    event.preventDefault();
    if (!selected || !attachmentFile || uploadingAttachment) return;
    const contentType = attachmentFile.type || "application/octet-stream";
    try {
      setUploadingAttachment(true);
      const attachment = await residentApi.prepareAttachment(selected.id, { fileName: attachmentFile.name, contentType, byteSize: attachmentFile.size });
      const uploadTarget = await residentApi.createAttachmentUploadTarget(selected.id, attachment.id);
      const uploadResponse = await fetch(uploadTarget.uploadUrl, { method: "PUT", headers: { "Content-Type": contentType }, body: attachmentFile });
      if (!uploadResponse.ok) throw new Error("附件上传未完成");
      await residentApi.completeAttachmentUpload(selected.id, attachment.id);
      setAttachmentFile(null); setAttachmentInputKey((value) => value + 1); await reloadSelected();
      onAnnounce("附件已上传并完成校验");
    } catch (error) { onAnnounce(error.message || "附件上传未完成"); } finally { setUploadingAttachment(false); }
  };

  const openAttachment = async (attachment, inline) => {
    if (!selected || attachment.uploadStatus !== "UPLOADED") return;
    try {
      const target = await residentApi.createAttachmentAccessTarget(selected.id, attachment.id, inline);
      const opened = window.open(target.url, "_blank", "noopener,noreferrer");
      if (!opened) onAnnounce("浏览器阻止了附件窗口，请允许此站点打开新窗口");
    } catch (error) { onAnnounce(error.message || "附件访问地址签发失败"); }
  };

  const submitRelatedPlan = async (event) => {
    event.preventDefault();
    if (!selected || !assessmentForPlan || !careApi) return;
    try {
      await careApi.createPlan({ ...relatedPlanForm, residentId: selected.id, assessmentId: assessmentForPlan.id, endDate: relatedPlanForm.endDate || undefined });
      setAssessmentForPlan(null); await reloadSelected(); onAnnounce("已基于评估创建护理计划草稿");
    } catch (error) { onAnnounce(error.message || "护理计划草稿未创建"); }
  };

  const canArchive = selected && (selected.status === "DISCHARGED" || selected.status === "DECEASED");

  return <section className="module-page resident-gateway-workspace">
    <div className="module-hero"><div><p className="eyebrow"><span />长者档案</p><h1>长者档案</h1><p>健康、评估、入住、护理执行与附件在当前机构范围内联查</p></div><button type="button" className="button primary" onClick={() => { setResidentForm(emptyResidentForm()); setShowCreate((value) => !value); }}><Plus size={17} />新增长者</button></div>

    {showCreate && <form className="panel resident-gateway-form" onSubmit={submitResident}><div className="panel-heading compact"><div><h2>新建档案</h2></div></div><ResidentFields value={residentForm} onChange={setResidentForm} /><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setShowCreate(false)}>取消</button><button type="submit" className="button primary">保存档案</button></div></form>}

    <div className="resident-gateway-grid"><section className="panel module-panel"><div className="module-toolbar"><label className="search-field"><Search size={15} /><input value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") void refresh(query); }} placeholder="搜索长者姓名" /></label><button type="button" className="text-action" onClick={() => void refresh(query)}>查询</button><span className="result-count">{records.length} 位</span></div><div className="module-table" style={{ "--columns": 4 }}>{loading ? <p className="table-empty">正在读取档案...</p> : <><div className="module-table-head"><span>长者</span><span>年龄</span><span>紧急联系人</span><span>状态</span><span>操作</span></div>{records.map((resident) => <div className="module-table-row" key={resident.id}><span className="record-main">{resident.name}</span><span className="record-value">{ageFromBirthDate(resident.birthDate)} 岁</span><span className="record-value">{resident.emergencyContactName}</span><span className="record-status">{STATUS_LABELS[resident.status] || resident.status}</span><span className="row-actions"><button type="button" aria-label={`查看${resident.name}`} onClick={() => void loadProfile(resident)}><Search size={16} /></button></span></div>)}</>}</div></section>

      <aside className="panel resident-profile-panel">{selected ? <><div className="panel-heading"><div><h2>{selected.name}</h2><p>{STATUS_LABELS[selected.status] || selected.status} · {ageFromBirthDate(selected.birthDate)} 岁</p></div><div className="resident-profile-actions"><button type="button" className="text-action" title="刷新 360 联查" onClick={() => void reloadSelected()}><UsersRound size={15} />联查</button><button type="button" className="text-action" title="编辑档案" onClick={() => { setResidentForm(residentFormFrom(selected)); setEditing(true); }}><Pencil size={15} />编辑</button>{canArchive && <button type="button" className="text-action danger" title="归档档案" onClick={() => setShowArchiveConfirmation(true)}><Archive size={15} />归档</button>}</div></div>
        {editing && <form className="resident-profile-edit" onSubmit={submitUpdate}><ResidentFields value={residentForm} onChange={setResidentForm} /><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setEditing(false)}>取消</button><button type="submit" className="button primary">保存修改</button></div></form>}
        {showArchiveConfirmation && <section className="resident-archive-confirmation"><h3>确认归档</h3><p>归档后，此档案将不再出现在常规查询结果中。</p><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setShowArchiveConfirmation(false)}>取消</button><button type="button" className="button destructive" onClick={() => void archiveResident()}>确认归档</button></div></section>}
        <section><div className="resident-section-heading"><h3>健康档案</h3><button type="button" className="text-action" onClick={() => { setHealthProfile(healthProfileFrom(healthProfile)); setEditingHealth((value) => !value); }}><Pencil size={14} />{editingHealth ? "收起" : "编辑"}</button></div>{editingHealth ? <form className="resident-profile-form health-profile-form" onSubmit={submitHealth}><HealthFields value={healthProfile} onChange={setHealthProfile} /><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setEditingHealth(false)}>取消</button><button type="submit" className="button primary">保存健康档案</button></div></form> : <div className="health-summary"><p><b>{healthProfile.careLevel || "未定级"}</b><span>照护等级 · {healthProfile.mobilityStatus || "行动未评定"} · {healthProfile.cognitionStatus || "认知未评定"}</span></p><p><b>重点风险</b><span>{[["营养", healthProfile.nutritionRisk], ["跌倒", healthProfile.fallRisk], ["压疮", healthProfile.pressureInjuryRisk], ["感染", healthProfile.infectionRisk]].filter(([, value]) => value).map(([label, value]) => `${label}${RISK_LABELS[value] || value}`).join(" · ") || "暂无评定"}</span></p><p><b>照护注意</b><span>{healthProfile.careNotes || "暂无"}</span></p></div>}</section>
        <section><div className="resident-section-heading"><h3>联系人</h3></div>{profile.contacts.map((contact) => <div className="profile-item" key={contact.id}><p><b>{contact.name}</b><span>{contact.relationshipText} · {contact.phone}{contact.primaryContact ? " · 主联系人" : ""}</span></p><div className="profile-item-actions"><button type="button" title="编辑联系人" onClick={() => { setContactForm(contactFormFrom(contact)); setEditingContactId(contact.id); }}><Pencil size={14} /></button><button type="button" title="删除联系人" onClick={() => setPendingDelete({ type: "contact", record: contact })}><Trash2 size={14} /></button></div></div>)}<form className="resident-profile-form" onSubmit={submitContact}><input required placeholder="姓名" value={contactForm.name} onChange={(event) => setContactForm((current) => ({ ...current, name: event.target.value }))} /><input required placeholder="关系" value={contactForm.relationshipText} onChange={(event) => setContactForm((current) => ({ ...current, relationshipText: event.target.value }))} /><input placeholder="联系电话" value={contactForm.phone} onChange={(event) => setContactForm((current) => ({ ...current, phone: event.target.value }))} /><label><input type="checkbox" checked={contactForm.primaryContact} onChange={(event) => setContactForm((current) => ({ ...current, primaryContact: event.target.checked }))} />主联系人</label><div className="inline-form-actions"><button type="submit" className="text-action">{editingContactId ? "保存联系人" : "添加联系人"}</button>{editingContactId && <button type="button" className="text-action muted" onClick={() => { setEditingContactId(null); setContactForm({ name: "", relationshipText: "", phone: "", primaryContact: false }); }}>取消</button>}</div></form></section>
        <section><div className="resident-section-heading"><h3>评估记录</h3></div>{profile.assessments.map((assessment) => <div className="profile-item" key={assessment.id}><p><b>{assessment.assessmentType} · {RISK_LABELS[assessment.riskLevel] || assessment.riskLevel}</b><span>{assessment.assessmentDate}{assessment.score !== null ? ` · ${assessment.score} 分` : ""}{assessment.note ? ` · ${assessment.note}` : ""}</span></p><div className="profile-item-actions"><button type="button" title="编辑评估" onClick={() => { setAssessmentForm(assessmentFormFrom(assessment)); setEditingAssessmentId(assessment.id); }}><Pencil size={14} /></button>{careApi && <button type="button" title="依据评估创建护理计划" onClick={() => { setAssessmentForPlan(assessment); setRelatedPlanForm({ planName: `${assessment.assessmentType}风险照护计划`, frequencyText: "每日", startDate: new Date().toISOString().slice(0, 10), endDate: "" }); }}><HeartPulse size={14} /></button>}<button type="button" title="删除评估" onClick={() => setPendingDelete({ type: "assessment", record: assessment })}><Trash2 size={14} /></button></div></div>)}<form className="resident-profile-form" onSubmit={submitAssessment}><input required placeholder="评估类型" value={assessmentForm.assessmentType} onChange={(event) => setAssessmentForm((current) => ({ ...current, assessmentType: event.target.value }))} /><input required type="date" value={assessmentForm.assessmentDate} onChange={(event) => setAssessmentForm((current) => ({ ...current, assessmentDate: event.target.value }))} /><input type="number" placeholder="评分" value={assessmentForm.score} onChange={(event) => setAssessmentForm((current) => ({ ...current, score: event.target.value }))} /><select value={assessmentForm.riskLevel} onChange={(event) => setAssessmentForm((current) => ({ ...current, riskLevel: event.target.value }))}><option value="LOW">低风险</option><option value="MEDIUM">中风险</option><option value="HIGH">高风险</option></select><input placeholder="观察说明" value={assessmentForm.note} onChange={(event) => setAssessmentForm((current) => ({ ...current, note: event.target.value }))} /><div className="inline-form-actions"><button type="submit" className="text-action">{editingAssessmentId ? "保存评估" : "添加评估"}</button>{editingAssessmentId && <button type="button" className="text-action muted" onClick={() => { setEditingAssessmentId(null); setAssessmentForm(assessmentFormFrom({})); }}>取消</button>}</div></form></section>
        <section><div className="resident-section-heading"><h3>附件</h3></div>{profile.attachments.map((attachment) => <div className="profile-item attachment-item" key={attachment.id}><p><b><FileText size={13} />{attachment.fileName}</b><span>{attachment.uploadStatus} · {attachment.contentType} · {attachment.byteSize} 字节</span></p>{attachment.uploadStatus === "UPLOADED" && <div className="profile-item-actions"><button type="button" title="预览附件" onClick={() => void openAttachment(attachment, true)}><Eye size={14} /></button><button type="button" title="下载附件" onClick={() => void openAttachment(attachment, false)}><Download size={14} /></button></div>}</div>)}<form className="resident-profile-form" onSubmit={submitAttachment}><input key={attachmentInputKey} required type="file" onChange={(event) => setAttachmentFile(event.target.files?.[0] || null)} /><button type="submit" className="text-action" disabled={!attachmentFile || uploadingAttachment}>{uploadingAttachment ? "正在上传" : "上传附件"}</button></form></section>
        {pendingDelete && <section className="resident-archive-confirmation"><div className="resident-section-heading"><h3>确认删除{pendingDelete.type === "contact" ? "联系人" : "评估记录"}</h3><button type="button" className="icon-button" title="关闭确认" onClick={() => setPendingDelete(null)}><X size={15} /></button></div><p>删除后不可恢复，请确认该记录不再需要。</p><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setPendingDelete(null)}>取消</button><button type="button" className="button destructive" onClick={() => void confirmDelete()}>确认删除</button></div></section>}
      </> : <div className="table-empty"><ClipboardCheck size={24} /><p>选择一位长者查看档案</p></div>}</aside></div>

    {selected && <section className="panel resident-360-panel"><div className="panel-heading"><div><p className="eyebrow"><span />长者 360</p><h2>{selected.name} 的服务关联</h2><p>评估依据、入住状态、护理计划、任务与服务记录在当前机构范围内汇总。</p></div><button type="button" className="text-action" onClick={() => void reloadSelected()}><UsersRound size={15} />刷新联查</button></div><div className="resident-360-grid"><article><h3>入住记录</h3>{profile.admissions.length ? profile.admissions.map((admission) => <p key={admission.id}><b>{admission.status}</b><span>{admission.roomNo || "待分配"} · {admission.bedNo || ""}</span></p>) : <p className="empty-inline">暂无入住记录</p>}</article><article><h3>护理计划</h3>{profile.carePlans.length ? profile.carePlans.map((plan) => <p key={plan.id}><b>{plan.planName}</b><span>{plan.status}{plan.assessmentId ? ` · 依据评估 #${plan.assessmentId}` : " · 未关联评估"}</span></p>) : <p className="empty-inline">暂无护理计划</p>}</article><article><h3>护理任务</h3>{profile.careTasks.length ? profile.careTasks.slice(0, 4).map((task) => <p key={task.id}><b>{task.taskName}</b><span>{task.status} · {String(task.scheduledAt || "").slice(0, 16)}</span></p>) : <p className="empty-inline">暂无护理任务</p>}</article><article><h3>服务记录</h3>{profile.serviceRecords.length ? profile.serviceRecords.slice(0, 4).map((record) => <p key={record.id}><b>{String(record.completedAt || "").slice(0, 10)}</b><span>{record.resultNote}</span></p>) : <p className="empty-inline">暂无服务记录</p>}</article></div></section>}

    {selected && assessmentForPlan && <form className="panel resident-gateway-form related-plan-form" onSubmit={submitRelatedPlan}><div className="panel-heading compact"><div><h2>依据评估创建护理计划草稿</h2><p>{assessmentForPlan.assessmentType} · {RISK_LABELS[assessmentForPlan.riskLevel] || assessmentForPlan.riskLevel}，计划将保留此评估依据引用。</p></div><button type="button" className="icon-button" title="关闭" onClick={() => setAssessmentForPlan(null)}><X size={17} /></button></div><div className="record-fields"><label>计划名称<input required value={relatedPlanForm.planName} onChange={(event) => setRelatedPlanForm((value) => ({ ...value, planName: event.target.value }))} /></label><label>服务频次<input required value={relatedPlanForm.frequencyText} onChange={(event) => setRelatedPlanForm((value) => ({ ...value, frequencyText: event.target.value }))} /></label><label>开始日期<input required type="date" value={relatedPlanForm.startDate} onChange={(event) => setRelatedPlanForm((value) => ({ ...value, startDate: event.target.value }))} /></label><label>结束日期<input type="date" value={relatedPlanForm.endDate} onChange={(event) => setRelatedPlanForm((value) => ({ ...value, endDate: event.target.value }))} /></label></div><div className="modal-actions"><button type="button" className="button secondary" onClick={() => setAssessmentForPlan(null)}>取消</button><button type="submit" className="button primary">创建计划草稿</button></div></form>}
  </section>;
}
