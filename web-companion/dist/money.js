export function moneyMinor(value) {
 const text=String(value).trim(); if(!/^\d{1,9}(\.\d{1,2})?$/.test(text)) throw Error('Enter a valid amount with up to two decimal places.');
 const [whole,fraction='']=text.split('.'); const amount=Number(whole)*100+Number(fraction.padEnd(2,'0')); if(!Number.isSafeInteger(amount)||amount>100000000000) throw Error('Amount is too large.'); return amount;
}
export function allocations(total, rows, method) {
 if(!rows.length) throw Error('Select at least one participant.');
 if(method==='equal') { const base=Math.floor(total/rows.length), extra=total%rows.length; return rows.map((r,i)=>({user_id:r.id,amount_minor:base+(i<extra?1:0)})); }
 if(method==='exact') {const out=rows.map(r=>({user_id:r.id,amount_minor:moneyMinor(r.value)})); if(out.reduce((sum,r)=>sum+r.amount_minor,0)!==total) throw Error('Exact shares must match the total.');return out;}
 const weights=rows.map(r=> method==='percentage'?moneyMinor(r.value):Number(r.value));
 if(weights.some(w=>!Number.isSafeInteger(w)||w<0||w>1000000)||weights.reduce((a,b)=>a+b,0)===0) throw Error('Enter valid non-negative weights.');
 const denominator=weights.reduce((a,b)=>a+b,0); if(method==='percentage'&&denominator!==10000) throw Error('Percentages must add to 100.');
 // Exact integer arithmetic; avoid floating-point rounding of currency allocations.
 const result=rows.map((r,i)=>({user_id:r.id,amount_minor:Number(BigInt(total)*BigInt(weights[i])/BigInt(denominator))}));
 let remainder=total-result.reduce((s,r)=>s+r.amount_minor,0); for(let i=0;remainder>0;i=(i+1)%rows.length){if(weights[i]>0){result[i].amount_minor++;remainder--;}} return result;
}
export function balances(members, payments, shares, settlements) {
 const map=Object.fromEntries(members.map(m=>[m.user_id,0]));
 payments.forEach(p=>{if(p.user_id in map) map[p.user_id]+=Number(p.amount_minor);});
 shares.forEach(p=>{if(p.user_id in map) map[p.user_id]-=Number(p.amount_minor);});
 settlements.filter(s=>s.status==='confirmed').forEach(s=>{if(s.payer_id in map)map[s.payer_id]+=Number(s.amount_minor);if(s.payee_id in map)map[s.payee_id]-=Number(s.amount_minor);}); return map;
}
