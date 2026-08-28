/* v2.1 campaign progression hotfix: surface is always deployable; sub-level doors also respect quest level requirements. */
portalUnlocked=function(t){if(state.campaignDone)return true;if(t==='foundry')return state.quest>=1&&state.level>=2;if(t==='cable')return state.quest>=2&&state.level>=4;if(t==='reactor')return state.quest>=3&&state.level>=6;return true;};
var _renderWorkshopCampaign=renderWorkshop;
renderWorkshop=function(){_renderWorkshopCampaign();$('startBtn').disabled=false;};