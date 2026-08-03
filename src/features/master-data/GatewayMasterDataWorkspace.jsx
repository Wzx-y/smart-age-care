import { useCallback, useEffect, useMemo, useState } from "react";
import { BedDouble, Building2, ClipboardList, Pencil, Plus, Power, Search, X } from "lucide-react";

const CATALOGS = [
  ["CARE_ITEM", "护理项目"],
  ["ASSESSMENT_TYPE", "评估类型"],
  ["RISK_LEVEL", "风险等级"],
  ["SHIFT", "班次"],
];

const emptyRoom = () => ({ building: "", floor: "", roomNo: "", roomType: "", nursingUnit: "" });
const emptyBed = () => ({ roomId: "", bedNo: "", equipmentSummary: "" });
const emptyItem = () => ({ itemCode: "", itemName: "", description: "", sortOrder: "0" });

function TabButton({ active, children, onClick }) {
  return <button type="button" className={active ? "filter active" : "filter"} onClick={onClick}>{children}</button>;
}

export function GatewayMasterDataWorkspace({ masterDataApi, onAnnounce }) {
  const [section, setSection] = useState("rooms");
  const [catalog, setCatalog] = useState("CARE_ITEM");
  const [includeDisabled, setIncludeDisabled] = useState(false);
  const [rows, setRows] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyRoom());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [nextRows, nextRooms] = await Promise.all([
        section === "rooms" ? masterDataApi.listRooms({ includeDisabled })
          : section === "beds" ? masterDataApi.listBeds({ includeDisabled })
            : masterDataApi.listCatalog(catalog, { includeDisabled }),
        masterDataApi.listRooms({ includeDisabled: false }),
      ]);
      setRows(nextRows);
      setRooms(nextRooms);
    } catch (error) {
      onAnnounce(error.message || "基础资料加载失败");
    } finally {
      setLoading(false);
    }
  }, [catalog, includeDisabled, masterDataApi, onAnnounce, section]);

  useEffect(() => { void load(); }, [load]);

  const startCreate = () => {
    setEditing({ mode: "create" });
    setForm(section === "rooms" ? emptyRoom() : section === "beds" ? emptyBed() : emptyItem());
  };
  const startEdit = (row) => {
    setEditing({ mode: "edit", row });
    setForm(section === "rooms"
      ? { building: row.building, floor: row.floor, roomNo: row.roomNo, roomType: row.roomType, nursingUnit: row.nursingUnit }
      : section === "beds"
        ? { roomId: String(row.roomId), bedNo: row.bedNo, equipmentSummary: row.equipmentSummary || "" }
        : { itemCode: row.itemCode, itemName: row.itemName, description: row.description || "", sortOrder: String(row.sortOrder) });
  };

  const submit = async (event) => {
    event.preventDefault();
    setWorking(true);
    try {
      if (section === "rooms") {
        const payload = { ...form };
        if (editing.mode === "edit") await masterDataApi.updateRoom(editing.row.id, { ...payload, version: editing.row.version });
        else await masterDataApi.createRoom(payload);
      } else if (section === "beds") {
        const payload = { ...form, roomId: Number(form.roomId) };
        if (editing.mode === "edit") await masterDataApi.updateBed(editing.row.id, { ...payload, version: editing.row.version });
        else await masterDataApi.createBed(payload);
      } else {
        const payload = { ...form, sortOrder: Number(form.sortOrder) };
        if (editing.mode === "edit") await masterDataApi.updateCatalogItem(catalog, editing.row.id, { itemName: payload.itemName, description: payload.description, sortOrder: payload.sortOrder, version: editing.row.version });
        else await masterDataApi.createCatalogItem(catalog, payload);
      }
      setEditing(null);
      await load();
      onAnnounce(editing.mode === "edit" ? "基础资料已更新" : "基础资料已创建");
    } catch (error) {
      onAnnounce(error.status === 409 ? "记录已变化或不满足停用条件，请刷新后重试" : (error.message || "基础资料未保存"));
      if (error.status === 409) await load();
    } finally {
      setWorking(false);
    }
  };

  const changeStatus = async (row) => {
    setWorking(true);
    try {
      const payload = { version: row.version, enabled: !row.enabled };
      if (section === "rooms") await masterDataApi.changeRoomStatus(row.id, payload);
      else if (section === "beds") await masterDataApi.changeBedStatus(row.id, payload);
      else await masterDataApi.changeCatalogItemStatus(catalog, row.id, payload);
      await load();
      onAnnounce(row.enabled ? "记录已停用" : "记录已启用");
    } catch (error) {
      onAnnounce(error.status === 409 ? "当前记录正在使用或已变化，无法修改状态" : (error.message || "状态未更新"));
      if (error.status === 409) await load();
    } finally {
      setWorking(false);
    }
  };

  const visibleRows = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return rows;
    return rows.filter((row) => Object.values(row).some((value) => String(value ?? "").toLowerCase().includes(keyword)));
  }, [query, rows]);
  const title = section === "rooms" ? "房间管理" : section === "beds" ? "床位管理" : CATALOGS.find(([id]) => id === catalog)?.[1] || "目录管理";

  return <section className="module-page master-data-workspace">
    <div className="module-hero"><div><p className="eyebrow"><span />机构基础资料</p><h1>基础资料管理</h1><p>房间、床位和照护目录只在当前机构范围内维护，停用规则由服务端校验。</p></div><button type="button" className="button primary" onClick={startCreate}><Plus size={17} />新建{title}</button></div>
    <section className="panel module-panel">
      <div className="module-toolbar"><div className="filter-row"><TabButton active={section === "rooms"} onClick={() => setSection("rooms")}><Building2 size={14} />房间</TabButton><TabButton active={section === "beds"} onClick={() => setSection("beds")}><BedDouble size={14} />床位</TabButton>{CATALOGS.map(([id, label]) => <TabButton key={id} active={section === "catalog" && catalog === id} onClick={() => { setSection("catalog"); setCatalog(id); }}><ClipboardList size={14} />{label}</TabButton>)}</div><label className="search-field"><Search size={15} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={`搜索${title}`} /></label><label className="inline-checkbox"><input type="checkbox" checked={includeDisabled} onChange={(event) => setIncludeDisabled(event.target.checked)} />显示已停用</label></div>
      {editing && <form className="panel resident-gateway-form" onSubmit={submit}><div className="panel-heading compact"><div><h2>{editing.mode === "edit" ? `编辑${title}` : `新建${title}`}</h2><p>保存时由服务端校验租户范围、唯一约束和版本。</p></div><button type="button" className="icon-button" onClick={() => setEditing(null)} aria-label="关闭"><X size={17} /></button></div>{section === "rooms" ? <div className="record-fields"><label>楼栋<input required value={form.building} onChange={(event) => setForm({ ...form, building: event.target.value })} /></label><label>楼层<input required value={form.floor} onChange={(event) => setForm({ ...form, floor: event.target.value })} /></label><label>房号<input required value={form.roomNo} onChange={(event) => setForm({ ...form, roomNo: event.target.value })} /></label><label>房型<input required value={form.roomType} onChange={(event) => setForm({ ...form, roomType: event.target.value })} /></label><label>护理单元<input required value={form.nursingUnit} onChange={(event) => setForm({ ...form, nursingUnit: event.target.value })} /></label></div> : section === "beds" ? <div className="record-fields"><label>所属房间<select required value={form.roomId} onChange={(event) => setForm({ ...form, roomId: event.target.value })}><option value="">请选择房间</option>{rooms.map((room) => <option key={room.id} value={room.id}>{room.building}-{room.floor}-{room.roomNo} · {room.nursingUnit}</option>)}</select></label><label>床位号<input required value={form.bedNo} onChange={(event) => setForm({ ...form, bedNo: event.target.value })} /></label><label>设备配置摘要<input value={form.equipmentSummary} onChange={(event) => setForm({ ...form, equipmentSummary: event.target.value })} /></label></div> : <div className="record-fields">{editing.mode === "create" && <label>目录编码<input required value={form.itemCode} onChange={(event) => setForm({ ...form, itemCode: event.target.value })} /></label>}<label>名称<input required value={form.itemName} onChange={(event) => setForm({ ...form, itemName: event.target.value })} /></label><label>说明<input value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><label>排序<input required type="number" min="0" value={form.sortOrder} onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} /></label></div>}<div className="modal-actions"><button type="button" className="button secondary" onClick={() => setEditing(null)}>取消</button><button type="submit" className="button primary" disabled={working}>{working ? "保存中" : "保存"}</button></div></form>}
      <div className="module-table" style={{ "--columns": 4 }}>{loading ? <p className="table-empty">正在读取{title}...</p> : <><div className="module-table-head"><span>{section === "rooms" ? "房间" : section === "beds" ? "床位" : "名称"}</span><span>{section === "rooms" ? "护理单元" : section === "beds" ? "占用 / 卫生" : "编码 / 排序"}</span><span>{section === "beds" ? "所属房间" : section === "rooms" ? "房型" : "说明"}</span><span>状态</span><span>操作</span></div>{visibleRows.length ? visibleRows.map((row) => <div className="module-table-row" key={row.id}><span className="record-main">{section === "rooms" ? `${row.building}-${row.floor}-${row.roomNo}` : section === "beds" ? row.bedNo : row.itemName}</span><span className="record-value">{section === "rooms" ? row.nursingUnit : section === "beds" ? `${row.occupancyStatus} / ${row.hygieneStatus}` : `${row.itemCode} / ${row.sortOrder}`}</span><span className="record-value">{section === "rooms" ? row.roomType : section === "beds" ? row.roomNo : row.description || "-"}</span><span className="record-status">{row.enabled ? "已启用" : "已停用"}</span><span className="row-actions"><button type="button" title="编辑" onClick={() => startEdit(row)}><Pencil size={15} /></button><button type="button" title={row.enabled ? "停用" : "启用"} className={row.enabled ? "danger" : ""} disabled={working} onClick={() => void changeStatus(row)}><Power size={15} /></button></span></div>) : <p className="table-empty">暂无{title}记录</p>}</>}</div>
    </section>
  </section>;
}
