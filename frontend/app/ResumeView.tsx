'use client';
import {useEffect,useState} from 'react';

export default function ResumeView(){
 const [resume,setResume]=useState<{filename?:string;content?:string}>({});
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
 return <section className="panel" style={{padding:28}}><h1>我的简历</h1><p>上传简历后，可在顾问问答中获取修改建议、岗位匹配和面试准备帮助。简历仅供你的账号使用，不进入公共知识库。</p><label>上传或替换简历 <input type="file" accept=".pdf,.docx,.txt,.md" disabled={busy} onChange={e=>void upload(e.target.files?.[0])}/></label><p>支持文字版 PDF、DOCX、TXT、MD，最大 10 MB；扫描件暂不支持。</p>{busy&&<p>正在识别简历…</p>}{error&&<p role="alert">{error}</p>}{resume.filename&&<><h2>{resume.filename}</h2><button disabled={busy} onClick={()=>{if(confirm('删除已保存的简历？'))void call('DELETE').catch(e=>setError(e.message))}}>删除简历</button><pre style={{whiteSpace:'pre-wrap',lineHeight:1.8}}>{resume.content}</pre></>}</section>
}
