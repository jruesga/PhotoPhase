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

'use strict';

var APP_VERSION = 1;

$.app_Configuration = new function () {
  this.debug = !(navigator.userAgent.indexOf('CrKey') != -1);
  this.weather = true;
  this.time = true;
  this.logo = true;
  this.track = true;
  this.cropCenter = true;
  this.blurredBackground = true;
  this.icon;
  this.name;
  this.label;
  this.device;
  this.loadingMsg = 'Loading';
};

$.app_Status = new function () {
  this.initialized = false;
  this.ready = false;
  this.started = false;
  this.foreground = false;
  this.currentTrackInfo = null;
  this.nextTrackInfo = null;
};

$.app_Request = function() {
  this.url;
  this.sender;
  this.token;
  this.title;
  this.album;
  this.width;
  this.height;
}

$.app_Message = function(type, msg) {
  this.type = type;
  this.msg = msg;
}

$.fn.app_onInitialize = function () {
  if (!$.app_Status.initialized) {
    $().time_initialize();
    $().weather_initialize();
    $().app_onConfigurationChanged($.app_Configuration);

    $('#photo-details').on('transitionend webkitTransitionEnd', function() {
      $.fn.app_onUpdateTrackInfo();
      if ($.app_Status.started) {
        $('#photo-details, #album-details').removeClass().addClass('fadein delayed800 duration800');
      } else {
        $('#photo-details, #album-details').removeClass().addClass('fadeout');
      }
    });

    $.app_Status.initialized = true;
  }
}

$.fn.app_onReady = function () {
  if (!$.app_Status.ready) {
    $.app_Status.ready = true;
    $().app_sendBroadcastMessage(new $.app_Message('ready', '{"v":' + APP_VERSION + '}'));
  }
}

$.fn.app_onConfigurationChanged = function (config) {
  $('#splash-icon, #appinfo-logo')
    .css('background', 'url(' + config.icon + ') no-repeat center')
    .css('background-size', 'cover');
  if (!$.app_Status.started) {
    $('#splash').waitForImages(function() {
      $('#splash').removeClass().addClass('fadein');

      // The reciever is ready in 1500ms. Notify to connected devices
      setTimeout('$().app_onReady();', 1500);
    }, $.noop, true);
  }
  $().time_configuration_changed(config);
  $().weather_configuration_changed(config);
  $('#splash-title').text(config.name);
  $('#splash-subtitle').text(config.label);
  $('#appinfo-name').text(config.name);
  $('#appinfo-device').text(config.device);
  $('#loading-text').text(config.loadingMsg);

  if (!config.logo) {
    $('#appinfo').css('display', 'none');
  } else {
    $('#appinfo').css('display', 'block');
  }
  if (!config.track) {
    $('#photo-details').css('display', 'none');
    $('#album-details').css('display', 'none');
  } else {
    $('#photo-details').css('display', 'block');
    $('#album-details').css('display', 'block');
  }
}

var pp_splash_transtion_registered = false;
$.fn.app_onStart = function (o) {
  if (!$.app_Status.started) {
    $.app_Status.started = true;
    $('#splash').removeClass().addClass('fadeout');
    if (pp_splash_transtion_registered) return;
    $('#splash').on('transitionend webkitTransitionEnd', function(){
      if ($('#splash').hasClass('fadeout')) {
        $.fn.app_onUpdateTrackInfo();
        if ($.app_Status.started) {
          $('#media').removeClass().addClass('fadein');
          o.removeClass().addClass('fadein');
          $('#backdrop').removeClass().addClass('fadein delayed');
          $().app_onNotifyTrackLoaded();
        } else {
          $('#media-fg-normal, #media-fg-blur, #media-bg-normal, #media-bg-blur')
            .css('background','url() no-repeat center');
        }
      }
    });
  }
}

var pp_backdrop_transtion_registered = false;
$.fn.app_onStop = function () {
  if ($.app_Status.started) {
    $.app_Status.started = false;
    $('#backdrop, #media').removeClass().addClass('fadeout');
    $.app_Status.currentTrackInfo = null;
    if (pp_backdrop_transtion_registered) return;
    $('#backdrop, #media').on('transitionend webkitTransitionEnd', function(){
      if ($('#backdrop').hasClass('fadeout')) {
        $('#splash').removeClass().addClass('fadein');

        $('#media-fg-blur').css('background','url() no-repeat center');
        $('#media-fg-normal').css('background','url() no-repeat center');
        $('#media-bg-blur').css('background','url() no-repeat center');
        $('#media-bg-normal').css('background','url() no-repeat center');
        $('#media-fg').removeClass().addClass('fadeout');
        $('#media-bg').removeClass().addClass('fadeout');
      }
    });
  }
}

$.fn.app_onRequest = function (request) {
  $().app_onShowLoading(true);
  $.app_Status.nextTrackInfo = request;
  setTimeout('$().app_onPerformRequest();', 300);
}

$.fn.app_onPerformRequest = function () {
  var ccratio = $(document).width() / $(document).height();
  var reqratio = $.app_Status.nextTrackInfo.width / $.app_Status.nextTrackInfo.height;
  var cover = $.app_Configuration.cropCenter && Math.abs(ccratio - reqratio) <= 0.5;
  var current = '#media-' + ($.app_Status.foreground ? 'fg' : 'bg');
  var next = '#media-' + ($.app_Status.foreground ? 'bg' : 'fg');
  var started = $.app_Status.started;
  $(next + '-normal')
    .css('background','url(' + $.app_Status.nextTrackInfo.url + ') no-repeat center')
    .css('background-size', cover ? 'cover' : 'contain');
  if (!cover && $.app_Configuration.blurredBackground) {
    $(next + '-blur')
      .css('background','url(' + $.app_Status.nextTrackInfo.url + ') no-repeat center')
      .css('background-size','cover');
  } else {
    $(next + '-blur')
      .css('background','#303030 no-repeat center');
  }
  $(next).waitForImages(function() {
    $().app_onShowLoading(false);

    $.app_Status.currentTrackInfo = $.app_Status.nextTrackInfo;

    if (!$.app_Status.started) {
      if (started == $.app_Status.started) {
        $.fn.app_onStart($(next));
      } else {
        $('#media-fg-normal, #media-fg-blur, #media-bg-normal, #media-bg-blur')
          .css('background','url() no-repeat center');
      }
    } else {
      $(current).removeClass().addClass('fadeout');
      if (started == $.app_Status.started) {
        $(next).removeClass().addClass('fadein');
        $().app_onNotifyTrackLoaded();
      } else {
        $('#media-fg-normal, #media-fg-blur, #media-bg-normal, #media-bg-blur')
          .css('background','url() no-repeat center');
      }
      $('#photo-details, #album-details').removeClass().addClass('fadeout duration500');
    }

    $.app_Status.foreground = !$.app_Status.foreground;
  }, function(loaded, count, success) {
    if (loaded == 0 && !success ) {
      $(next + '-normal')
        .css('background','url(404.jpg) no-repeat center')
        .css('background-size', 'cover');
    }
  }, true);
}

$.fn.app_onShowLoading = function (visible) {
    if (visible) {
      $('#loading > div').addClass('on');
      $('#loading').fadeIn(250);
    } else {
      $('#loading').fadeOut(150, function() {
        $('#loading > div').removeClass('on');
      });
    }
}

$.fn.app_onUpdateTrackInfo = function () {
  if ($.app_Status.currentTrackInfo != null) {
    $('#photo-details').html($.app_Status.currentTrackInfo.title);
    $('#album-details').html($.app_Status.currentTrackInfo.album);
  }
}

$.fn.app_onNotifyTrackLoaded = function () {
  var msg = '{"k":"' + $.app_Status.currentTrackInfo.token + '","s":"' + $.app_Status.currentTrackInfo.sender + '"}';
  setTimeout('$().app_sendBroadcastMessage(new $.app_Message(\'track\', \'' +  msg + '\'));', 2000);
}

$.fn.app_onNewEvent = function (senderId, event) {
  var type = event['type'];
  if (type == 'conf') {
    // Configuration
    if (event.hasOwnProperty('w')) $.app_Configuration.weather = event['w'];
    if (event.hasOwnProperty('t')) $.app_Configuration.time = event['t'];
    if (event.hasOwnProperty('lg')) $.app_Configuration.logo = event['lg'];
    if (event.hasOwnProperty('tr')) $.app_Configuration.track = event['tr'];
    if (event.hasOwnProperty('i')) $.app_Configuration.icon = event['i'];
    if (event.hasOwnProperty('n')) $.app_Configuration.name = event['n'];
    if (event.hasOwnProperty('l')) $.app_Configuration.label = event['l'];
    if (event.hasOwnProperty('d')) $.app_Configuration.device = event['d'];
    if (event.hasOwnProperty('cc')) $.app_Configuration.cropCenter = event['cc'];
    if (event.hasOwnProperty('bb')) $.app_Configuration.blurredBackground = event['bb'];
    if (event.hasOwnProperty('ml')) $.app_Configuration.loadingMsg = event['ml'];
    $().app_onConfigurationChanged($.app_Configuration);
  } else if (type == 'cast') {
    // Cast request
    var request = new $.app_Request();
    request.url = event['u'];
    request.sender = event['s'];
    request.token = event['k'];
    request.title = event['t'];
    request.album = event['a'];
    request.width = event['w'];
    request.height = event['h'];
    $().app_onRequest(request);
  } else if (type == 'ping') {
    // Ping
    $().app_sendMessage(event.senderId, new $.app_Message('pong','{}'));
  } else if (type == 'stop') {
    // Stop cast
    if ($.app_Status.started) {
      $().app_onStop();
    }
  }
}

$.fn.app_sendBroadcastMessage = function (msg) {
  if (!$.app_Configuration.debug) {
    window.castMessageBus.broadcast(JSON.stringify(msg))
  }
}

$.fn.app_sendMessage = function (to, msg) {
  if (!$.app_Configuration.debug) {
    window.castMessageBus.send(to, JSON.stringify(msg))
  }
}

// Chromecast Receiver
if (!$.app_Configuration.debug) {
  window.castMediaPlayer = {
    editTracksInfo: function (data) {},
    getCurrentTimeSec: function () {return 0;},
    getDurationSec: function () {return 0;},
    getState: function () {return cast.receiver.media.PlayerState.IDLE;},
    getVolume: function () {
      var volume = new cast.receiver.media.Volume();
      volume.level = 0;
      volume.muted = false;
      return volume;
    },
    load: function (contentId, autoplay, time, tracksInfo, onlyLoadTracks) {},
    pause: function () {},
    play: function () {},
    registerEndedCallback: function (cb) {},
    registerErrorCallback: function (cb) {},
    registerLoadCallback: function (cb) {},
    reset: function () {},
    seek: function (time, resumeState) {},
    setVolume: function (volume) {},
    unregisterEndedCallback: function () {},
    unregisterErrorCallback: function () {},
    unregisterLoadCallback: function () {}
  };
  window.castMediaManager = new cast.receiver.MediaManager(window.castMediaPlayer);


  window.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();
  window.castReceiverManager.onReady = function(event) {
    $().app_onInitialize();
  };
  window.castReceiverManager.onSenderConnected = function(event) {
    if ($.app_Status.ready) {
      $().app_sendMessage(event.senderId, new $.app_Message('ready', '{"v":' + APP_VERSION + '}'));
    }
  };
  window.castReceiverManager.onSenderDisconnected = function(event) {
    if(window.castReceiverManager.getSenders().length == 0 &&
        event.reason == cast.receiver.system.DisconnectReason.REQUESTED_BY_SENDER) {
      window.close();
    }
  };
  window.castMessageBus = window.castReceiverManager.getCastMessageBus('urn:x-cast:com.ruesga.android.wallpapers.photophase');
  window.castMessageBus.onMessage = function(event) {
    $().app_onNewEvent(event.senderId, JSON.parse(event.data));
  }

  window.castReceiverManager.start();
}
