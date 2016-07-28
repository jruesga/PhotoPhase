/*
 * Copyright (C) 2016 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"use strict";

$.fn.time_tt = 0;

$.fn.time_configuration_changed = function (config) {
  if (!config.time) {
    $('#time-block').css('display', 'none');
    clearTimeout($.fn.time_tt);
    $.fn.time_tt = 0;
  } else {
    $().time_tick();
    $('#time-block').css('display', 'inline-block');
  }
}

$.fn.time_initialize = function () {
}

$.fn.time_tick = function () {
  var locale = window.navigator.userLanguage || window.navigator.language;
  var now = moment();
  var next = moment().startOf('minute').add(1, 'minute');
  $('#time').text(now.locale(locale).format('LT'));

  if ($.fn.time_tt != 0) {
    clearTimeout($.fn.time_tt);
    $.fn.time_tt = 0;
  }
  if ($.app_Configuration.time) {
    $.fn.time_tt = setTimeout($().time_tick, next.diff(now));
  }
};
