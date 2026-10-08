import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateDraft } from '../supabase/functions/read-poster/index.ts';
const base = {title:'IPL FAN PARK',originalTitle:'IPL FAN PARK',venue:'Ground',locality:'Mangalore',summary:'',dates:[{year:null,month:5,day:29},{year:null,month:5,day:31}],dateRelationship:'listed',startTimes:[],endTime:null,evidence:'May 29 & 31',warnings:[]};
test('keeps omitted year and separated dates, never fills end time',()=>{ const d=validateDraft(base);assert.deepEqual(d.dates,base.dates);assert.equal(d.endTime,null);assert.deepEqual(d.startTimes,[]); });
test('preserves 10 circus dates and two shows',()=>{const dates=[22,23,26,27,28,29,30].map(day=>({year:null,month:8,day})).concat([4,5,6].map(day=>({year:null,month:9,day}))); const d=validateDraft({...base,dates,startTimes:['17:30','20:00']});assert.equal(d.dates.length,10);assert.deepEqual(d.startTimes,['17:30','20:00']);});
test('historical date is preserved and flagged',()=>{const d=validateDraft({...base,dates:[{year:2022,month:4,day:23}]});assert.equal(d.dates[0].year,2022);assert.ok(d.warnings.some(w=>w.includes('past date')));});
test('invalid dates/times and oversized lists rejected',()=>{for(const dates of [[{year:2026,month:2,day:30}],[{year:1800,month:1,day:1}],Array(32).fill(base.dates[0])]) assert.throws(()=>validateDraft({...base,dates})); for(const startTimes of [['25:00'],['5:30 PM']]) assert.throws(()=>validateDraft({...base,startTimes}));});
