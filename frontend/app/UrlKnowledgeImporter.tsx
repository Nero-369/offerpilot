'use client';

import {useState} from 'react';
import './url-import.css';

type Preview={sourceUrl:string;title?:string;city?:string;policyType?:string;effectiveDate?:string;expiryDate?:string;versionLabel?:string;authorityLevel:number;content?:string;error?:string};

export default function UrlKnowledgeImporter({onDone}:{onDone:()=>void}) {
  const [open,setOpen]=useState(false),[urls,setUrls]=useState(''),[items,setItems]=useState<Preview[]>([]);
  const [busy,setBusy]=useState(false),[message,setMessage]=useState('');

  async function preview(){
    const values=urls.split(/\r?\n/).map(value=>value.trim()).filter(Boolean);
    if(!values.length)return setMessage('请至少粘贴一个网址');
    setBusy(true);setMessage('正在下载并识别正文…');
    try{
      const response=await fetch('/api/v1/knowledge/url-preview',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({urls:values})});
      if(!response.ok)throw new Error(await response.text());
      setItems(await response.json());setMessage('请检查识别结果，确认后再入库');
    }catch(error){setMessage(`读取失败：${error instanceof Error?error.message:'未知错误'}`)}finally{setBusy(false)}
  }

  function update(index:number,patch:Partial<Preview>){setItems(current=>current.map((item,i)=>i===index?{...item,...patch}:item))}

  async function commit(){
    const valid=items.filter(item=>!item.error&&item.title&&item.content);
    if(!valid.length)return setMessage('没有可以入库的文档');
    setBusy(true);let completed=0;
    try{
      for(const item of valid){
        const response=await fetch('/api/v1/knowledge',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(item)});
        if(!response.ok)throw new Error(`${item.title}：${await response.text()}`);
        completed++;
      }
      setMessage(`成功导入 ${completed} 篇文档`);setItems([]);setUrls('');onDone();
    }catch(error){setMessage(`已导入 ${completed} 篇，随后失败：${error instanceof Error?error.message:'未知错误'}`)}finally{setBusy(false)}
  }

  return <>
    <button className="url-import-trigger" onClick={()=>setOpen(true)}>🔗 从网址自动导入</button>
    {open&&<div className="modal-backdrop"><section className="modal url-import-modal">
      <button className="close" onClick={()=>setOpen(false)}>×</button><span className="eyebrow">URL AUTO IMPORT</span>
      <h2>从网址批量导入知识</h2><p>每行粘贴一个政府网页或 PDF，最多20个。系统先生成预览，不会直接写入知识库。</p>
      {!items.length?<><label>政策网址<textarea value={urls} onChange={event=>setUrls(event.target.value)} placeholder={'https://...\nhttps://...'}/></label><button className="modal-primary" disabled={busy} onClick={preview}>{busy?'正在识别…':'抓取并预览 →'}</button></>:<>
        <div className="url-preview-list">{items.map((item,index)=><article className={item.error?'failed':''} key={item.sourceUrl}>
          {item.error?<><b>读取失败</b><p>{item.sourceUrl}</p><em>{item.error}</em></>:<>
            <label>标题<input value={item.title||''} onChange={event=>update(index,{title:event.target.value})}/></label>
            <div><label>城市<input value={item.city||''} onChange={event=>update(index,{city:event.target.value})}/></label><label>政策类型<input value={item.policyType||''} onChange={event=>update(index,{policyType:event.target.value})}/></label></div>
            <div><label>生效日期<input type="date" value={item.effectiveDate||''} onChange={event=>update(index,{effectiveDate:event.target.value||undefined})}/></label><label>文号/版本<input value={item.versionLabel||''} onChange={event=>update(index,{versionLabel:event.target.value})}/></label></div>
            <small>{item.content?.length.toLocaleString()} 字 · {item.sourceUrl}</small>
          </>}
        </article>)}</div>
        <div className="url-import-actions"><button onClick={()=>setItems([])}>返回修改网址</button><button disabled={busy} onClick={commit}>{busy?'正在建立索引…':'确认并批量入库'}</button></div>
      </>}
      {message&&<div className="url-import-message">{message}</div>}
    </section></div>}
  </>
}
