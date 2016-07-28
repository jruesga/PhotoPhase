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

$.weather_latlong = null;
$.fn.weather_tt = 0;

$.fn.weather_configuration_changed = function (config) {
  if (!config.weather) {
    $('#weather-block-1').css('display', 'none');
    $('#weather-block-2').css('display', 'none');
    clearTimeout($.fn.weather_tt);
    $.fn.weather_tt = 0;
  } else {
    if ($.fn.weather_tt == 0) {
      if ($.weather_latlong == null) {
          $().weather_requestLocation();
      } else {
          $().weather_requestWeather();
      }
    }
    $('#weather-block-1').css('display', 'inline-block');
    $('#weather-block-2').css('display', 'inline-block');
  }
}

$.fn.weather_initialize = function () {
}

$.fn.weather_requestLocation = function () {
  if (!$.app_Configuration.debug && $.app_Configuration.weather) {
    $.ajax({
      url: 'http://geoip.nekudo.com/api/',
      type: 'POST',
      dataType: 'jsonp',
      success: function(data) {
        $.fn.weather_tt = 0;
        $.weather_latlong = [data.location.latitude,data.location.longitude];
        $().weather_requestWeather();
      },
      error: function (xhr, ajaxOptions, thrownError) {
        if ($.fn.weather_tt != 0) {
          clearTimeout($.fn.weather_tt);
          $.fn.weather_tt = 0;
        }
        $.fn.weather_tt = setTimeout('$().weather_requestLocation()', 60000);
      }
    });
  } else {
    $().weather_requestWeather();
  }
}

$.fn.weather_requestWeather = function () {
  if (!$.app_Configuration.debug) {
    var locale = window.navigator.userLanguage || window.navigator.language;
    $.simpleWeather({
      location: $.weather_latlong[0]+','+$.weather_latlong[1],
      unit: $.inArray(locale.toLowerCase(), ['en-us', 'en-ba', 'en-bz', 'en-pw']) ? 'c' : 'f',
      success: $().onWeatherSuccess = function(weather) {
        $.fn.weather_tt = 0;
        $('#weather-icon-symbol').removeClass().addClass('wi wi-yahoo-'+weather.code);
        $('#weather-temp').html(weather.temp+'&#176;'+weather.units.temp);
        $('#weather-maxmin').html(weather.high+'&#176;|'+weather.low+'&#176;');
        if ($.fn.weather_tt != 0) {
          clearTimeout($.fn.weather_tt);
          $.fn.weather_tt = 0;
        }
        $.fn.weather_tt = setTimeout('$().weather_requestWeather()', 3600000);
      },
      error: $().onWeatherSuccess = function(error) {
        if ($.fn.weather_tt != 0) {
          clearTimeout($.fn.weather_tt);
          $.fn.weather_tt = 0;
        }
        $.fn.weather_tt = setTimeout('$().weather_requestWeather()', 60000);
      }
    });
  } else {
    $.fn.weather_tt = 0;
    $('#weather-icon-symbol').removeClass().addClass('wi wi-yahoo-30');
    $('#weather-temp').html('38&#176;C');
    $('#weather-maxmin').html('32&#176;|15&#176;');
  }
}