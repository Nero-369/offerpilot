'use client';
import {useEffect,useState} from 'react';
import './resume.css';

type Resume={filename?:string;content?:string;updatedAt?:string};
export default function ResumeView(){
 const [resume,setResume]=useState<Resume>({});
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 async function call(method='GET',body?:FormData){
  const token=decodeURIComponent(document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)?.[1]??'');
  const r=await fetch('/api/v1/resume',{method,body,credentials:'include',headers:method==='GET'?{}:{'X-XSRF-TOKEN':token}});
  if(!r.ok)throw new Error('简历操作失败，请稍后重试');
  if(method==='DELETE'){setResume({});return}
  setResume(await r.json());
 }
 useEffect(()=>{const timer=window.setTimeout(()=>void call().catch(e=>setError(e.message)),0);return()=>window.clearTimeout(timer)},[]);
 async function upload(file?:File){if(!file)return;setBusy(true);setError('');try{const body=new FormData();body.append('file',file);await call('POST',body)}catch(e){setError(e instanceof Error?e.message:'上传失败')}finally{setBusy(false)}}
 const updated=resume.updatedAt?new Date(resume.updatedAt).toLocaleString('zh-CN',{dateStyle:'medium',timeStyle:'short'}):'';
 return <section className="resume-page">
  <header className="resume-header"><div><span>个人职业档案</span><h1>我的简历</h1><p>集中管理你的求职资料，顾问会据此提供简历修改、岗位匹配和面试准备建议。</p></div><div className="resume-private"><i>✓</i><div><b>仅自己可见</b><small>资料与账号隔离保存</small></div></div></header>
  {error&&<div className="resume-alert" role="alert">{error}</div>}
  <div className="resume-grid"><section className="resume-upload-card"><div className="resume-upload-icon">↥</div><h2>{resume.filename?'更新你的简历':'上传你的第一份简历'}</h2><p>上传后自动提取文字内容，随时可以替换。建议使用排版清晰的文字版文件。</p><label className={busy?'busy':''}>{busy?'正在识别文件…':resume.filename?'选择新文件并替换':'选择文件上传'}<input type="file" accept=".pdf,.docx,.txt,.md" disabled={busy} onChange={e=>void upload(e.target.files?.[0])}/></label><small>支持 PDF、DOCX、TXT、MD · 最大 10 MB</small><div className="resume-file-tips"><span>✓ 自动识别文字</span><span>✓ 用于个性化建议</span><span>✓ 可随时删除</span></div></section>
  <aside className="resume-guide"><span>使用建议</span><h2>让分析结果更贴近你</h2>{[['01','保持经历完整','包含教育、实习、项目与技能信息。'],['02','优先使用文字版','扫描件图片目前无法准确提取内容。'],['03','及时更新版本','投递方向变化时，替换为最新版本。']].map(x=><div key={x[0]}><i>{x[0]}</i><p><b>{x[1]}</b><small>{x[2]}</small></p></div>)}</aside></div>
  {resume.filename?<section className="resume-document"><header><div className="resume-doc-icon">DOC</div><div><span>当前简历</span><h2>{resume.filename}</h2><p>{updated?`最近更新于 ${updated}`:'已保存到你的个人档案'}</p></div><button disabled={busy} onClick={()=>{if(confirm('删除已保存的简历？'))void call('DELETE').catch(e=>setError(e.message))}}>删除</button></header><div className="resume-preview"><div><b>内容预览</b><span>已提取 {(resume.content?.length??0).toLocaleString()} 个字符</span></div><pre>{resume.content}</pre></div></section>:<section className="resume-empty"><span>◇</span><div><h2>还没有已保存的简历</h2><p>上传后将在这里展示文字预览，你可以随时更新版本。</p></div></section>}
 </section>
}
