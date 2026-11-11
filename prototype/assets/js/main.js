/* 启动 */
(function () {
  'use strict';
  function boot() {
    if (!location.hash) location.hash = '#/';
    Router.render();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
