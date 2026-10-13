// Run only with approval to create AND clean up these disposable verification records.
import fs from 'node:fs/promises';import vm from 'node:vm';import assert from 'node:assert/strict';import {randomUUID} from 'node:crypto';
if(process.env.POI_APPROVED_LINK_TEST!=='1')throw Error('Live temporary-account verification requires cleanup approval.');
if(process.env.NODE_TLS_REJECT_UNAUTHORIZED==='0')throw Error('TLS validation must remain enabled.');
const createClient=vm.runInThisContext((await fs.readFile(new URL('../web-companion/dist/vendor/supabase.js',import.meta.url),'utf8'))+'\nsupabase.createClient');
const config=Object.fromEntries((await fs.readFile(new URL('../supabase.properties',import.meta.url),'utf8')).split(/\r?\n/).filter(l=>l.includes('=')&&!l.startsWith('#')).map(l=>[l.slice(0,l.indexOf('=')).trim(),l.slice(l.indexOf('=')+1).trim()]));
const clients=[],users=[],paths=[];let event;
const ok=async p=>{const {data,error}=await p;if(error)throw error;return data;};
try{
 for(const label of ['Owner','Guest']){const c=createClient(config.url,config.publishableKey,{auth:{persistSession:false,autoRefreshToken:false}});clients.push(c);const signup=await ok(c.auth.signUp({email:`poi.link.verify.${randomUUID()}@example.com`,password:`PoiLink!${randomUUID()}`,options:{data:{display_name:`Temporary link ${label}`}}}));assert.ok(signup.session);users.push(signup.user);}
 const [a,b]=clients,[alice,bob]=users;event=await ok(a.from('events').insert({title:'Temporary link verification',summary:'Disposable event join test',description:'Temporary verification, removed after test',category:'community',starts_at_millis:Date.now()+86400000,ends_at_millis:Date.now()+90000000,venue:'Test venue',address:'Test only',organizer_name:'Temporary owner',visibility:'invite_only',theme_key:'community',created_by:alice.id}).select().single());
 assert.equal((await ok(b.from('events').select('id').eq('id',event.id))).length,0);
 assert.equal((await b.rpc('create_poi_event_join_link',{p_event_id:event.id})).error!==null,true);
 const token=await ok(a.rpc('create_poi_event_join_link',{p_event_id:event.id}));assert.match(token,/^[a-f0-9]{64}$/);
 assert.equal((await b.rpc('join_poi_event_link',{p_token:'0'.repeat(64)})).error!==null,true);
 assert.equal(await ok(b.rpc('join_poi_event_link',{p_token:token})),event.id);
 assert.equal(await ok(b.rpc('join_poi_event_link',{p_token:token})),event.id);
 assert.equal((await ok(b.from('events').select('id').eq('id',event.id))).length,1);
 assert.equal((await ok(b.from('attendance').select('event_id').eq('event_id',event.id))).length,0);
 const id=randomUUID(),path=`${event.id}/${bob.id}/${id}.png`;paths.push(path);await ok(b.storage.from('event-moments').upload(path,Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aE1cAAAAASUVORK5CYII=','base64'),{contentType:'image/png'}));await ok(b.from('event_moments').insert({id,event_id:event.id,author_id:bob.id,author_name:'Temporary guest',image_path:path,caption:'Uploaded without attendance',created_at_millis:Date.now()}));
 await ok(a.from('event_expense_groups').insert({event_id:event.id,created_by:alice.id,currency:'INR'}));assert.equal((await ok(b.from('event_expense_groups').select('id').eq('event_id',event.id))).length,1);
 const replacement=await ok(a.rpc('create_poi_event_join_link',{p_event_id:event.id}));assert.notEqual(token,replacement);assert.ok((await b.rpc('join_poi_event_link',{p_token:token})).error);
 await ok(a.rpc('revoke_poi_event_join_link',{p_event_id:event.id}));assert.ok((await b.rpc('join_poi_event_link',{p_token:replacement})).error);assert.equal((await ok(b.from('events').select('id').eq('id',event.id))).length,1);
 console.log('PASS: non-owner/invalid links blocked; guest joins exact private event without friendship; repeated join idempotent; photo upload without attendance; expenses stay private; rotation/revocation blocks old tokens without removing joined members.');
}finally{
 if(paths.length&&clients[1])await ok(clients[1].storage.from('event-moments').remove(paths));
 if(event&&clients[0])await ok(clients[0].from('events').delete().eq('id',event.id).eq('created_by',users[0].id));
 for(const c of clients){if((await c.auth.getSession()).data.session){await ok(c.rpc('delete_poi_account'));console.log('Removed exact temporary link verification account.');}await c.auth.signOut();}
}
