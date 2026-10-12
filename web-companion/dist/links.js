export const companionOrigin='https://poi-trip-companion.dhanushriyan22.chatgpt.site';
export const validEventId=value=>/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i.test(value||'');
export const validJoinToken=value=>/^[a-f0-9]{64}$/.test(value||'');
export function readEventLink(url){const u=new URL(url),hash=new URLSearchParams(u.hash.slice(1));return {eventId:validEventId(u.searchParams.get('event'))?u.searchParams.get('event'):'',token:validJoinToken(hash.get('join'))?hash.get('join'):''};}
export function eventLink(eventId,token=''){if(!validEventId(eventId))throw Error('Invalid event ID.');if(token&&!validJoinToken(token))throw Error('Invalid join token.');return `${companionOrigin}/?event=${eventId}${token?'#join='+token:''}`;}
