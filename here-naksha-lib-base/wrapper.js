var module = {exports: {}};
var exports = module.exports;

function js() {
  let js = module.exports["here-naksha-lib-geo"].com.here.naksha.js;
  js.JsEngine.Companion.initJsEngine();
  js.TestFactory = {
    id: function() { return "TestFactory" },
    create: function (o) {
      return {
        _raw: o,
        raw: function() {
          return this._raw;
        }
      };
    }
  };
  return js;
}
// Put code HERE:



(()=>{var t={788:function(t,n,e){var r,o,i;o=[n,e(675)],void 0===(i="function"==typeof(r=function(t,n){"use strict";var e,r=n.$_$.h,o=n.$_$.e,i=n.$_$.i,a=n.$_$.a,u=(n.$_$.b,n.$_$.c),s=n.$_$.f,c=n.$_$.g,l=n.$_$.d;function f(){}function h(){}function $(){}function m(){}function p(){}function y(){}function b(){e=this,this.e_1=!1,this.arrayTemplate=new m,this.objectTemplate=new p,this.symbolTemplate=new y,this.UNDEFINED=void 0,this.ITERATOR=Symbol.iterator}function v(){return null==e&&new b,e}function j(){v()}return i(h,"JsObject",o),i(f,"JsArray",o,a,[h]),i((function(){}),"JsIterator",o),i((function(){}),"JsIteratorResult",o),i($,"JsSymbol",o),i((function(){}),"NkAccessor",o),i((function(){}),"NkAccessorFactory",o),i(m,a,u,a,[f]),i(p,a,u,a,[h]),i(y,a,u,a,[$]),i(b,"Companion",c),i(j,"JsEngine",u,a,a,j),r(m).get=function(t){return this.get(t)},r(m).set=function(t,n){var e=this.get(t);return this.set(t,n),e},r(b).f=function(){return this.arrayTemplate},r(b).g=function(){return this.objectTemplate},r(b).h=function(){return this.symbolTemplate},r(b).copy=function(t,n){return function(){var e,r=Object.getPrototypeOf(n),o=Object.getPrototypeOf(t),i=Object.getOwnPropertySymbols(o);for(e in i){var a=i[e];r[a]=o[a]}var u={enumerable:!1,writable:!0,value:null},s=Object.getOwnPropertyNames(o);for(e in s){var c=s[e];u.value=o[c],Object.defineProperty(r,c,u)}}.call(this)},r(b).initJsEngine=function(){if(!this.e_1){this.e_1=!0;this.copy(this.arrayTemplate,[]);this.copy(this.objectTemplate,{});var t=Symbol();this.copy(this.symbolTemplate,t)}},r(b).i=function(){return this.UNDEFINED},r(b).containsKey=function(t,n){return Object.hasOwn(t,n)},r(b).get=function(t,n){return t[n]},r(b).set=function(t,n,e){return function(){return t[n],t[n]=e,t}.call(this)},r(b).delete=function(t,n){return function(){var e=t[n];return delete t[n],e}.call(this)},r(b).symbol=function(t){return function(){return Symbol[t]||(Symbol[t]=Symbol(t))}.call(this)},r(b).query=function(t,n){var e=this.symbol(n.id()),r=this.get(t,e);return null==r&&(r=n.create(t),this.set(t,e,r)),r},r(b).j=function(){return this.ITERATOR},r(b).iterator=function(t){return t[Symbol.iterator]()},r(b).newObject=function(){return{}},r(b).newArray=function(){return[]},r(b).isObject=function(t){return null!=t&&s(t,h)},r(b).isArray=function(t){return null!=t&&s(t,f)},r(b).isSymbol=function(t){return null!=t&&s(t,$)},function(t){var n,e,r,o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={});o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={}),o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={}),o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={}),o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={}),o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={}),o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={}),(o=(r=(e=(n=t.com||(t.com={})).here||(n.here={})).naksha||(e.naksha={})).js||(r.js={})).JsEngine=j,l(o.JsEngine,"Companion",v)}(t),t})?r.apply(n,o):r)||(t.exports=i)},675:function(t,n){var e,r;void 0===(r="function"==typeof(e=function(t){"use strict";function n(){}var e,r,o;function i(t){for(var n=1,e=[],r=0,o=t.length;r<o;){var i=t[r];r=r+1|0;var a=n,u=i.prototype.$imask$,s=null==u?i.$imask$:u;null!=s&&(e.push(s),a=s.length);var c=i.$metadata$.iid,l=null==c?null:(h=void 0,$=void 0,m=void 0,h=(f=c)>>5,$=new Int32Array(h+1|0),m=1<<(31&f),$[h]=$[h]|m,$);null!=l&&(e.push(l),a=Math.max(a,l.length)),a>n&&(n=a)}var f,h,$,m;return function(t,n){for(var e=0,r=new Int32Array(t);e<t;){for(var o=e,i=0,a=0,u=n.length;a<u;){var s=n[a];a=a+1|0,o<s.length&&(i|=s[o])}r[o]=i,e=e+1|0}return r}(n,e)}function a(t,n,e,r,i,a,u){return{kind:t,simpleName:n,associatedObjectKey:r,associatedObjects:i,suspendArity:a,$kClass$:o,defaultConstructor:e,iid:u}}function u(t,n,e,r,o,a,u,s,c){null!=r&&(t.prototype=Object.create(r.prototype),t.prototype.constructor=t);var l=e(n,a,u,s,null==c?[]:c);t.$metadata$=l,null!=o&&((null!=l.iid?t:t.prototype).$imask$=i(o))}function s(t,n,e,r,o){return a("object",t,n,e,r,o,null)}return u(n,"Unit",s),e=new n,t.$_$=t.$_$||{},t.$_$.a=o,t.$_$.b=e,t.$_$.c=function(t,n,e,r,o){return a("class",t,n,e,r,o,null)},t.$_$.d=function(t,n,e,r){return Object.defineProperty(t,n,{configurable:!0,get:e,set:r})},t.$_$.e=function(t,n,e,i,u){return a("interface",t,n,e,i,u,(r===o&&(r=0),r=r+1|0))},t.$_$.f=function(t,n){return function(t,n){var e=t.$imask$;return null!=e&&function(t,n){var e=n>>5;if(e>t.length)return!1;var r=1<<(31&n);return!(0==(t[e]&r))}(e,n)}(t,n.$metadata$.iid)},t.$_$.g=s,t.$_$.h=function(t){return t.prototype},t.$_$.i=u,t})?e.apply(n,[n]):e)||(t.exports=r)}},n={},e=function e(r){var o=n[r];if(void 0!==o)return o.exports;var i=n[r]={exports:{}};return t[r].call(i.exports,i,i.exports,e),i.exports}(788);module.exports["here-naksha-lib-js"]=e})();
