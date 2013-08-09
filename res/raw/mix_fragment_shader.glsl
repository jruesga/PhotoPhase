/*
 * Copyright (C) 2013 The CyanogenMod Project
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

precision mediump float;

varying vec2 vTextureCoord;
uniform sampler2D sTexture1;
uniform sampler2D sTexture2;

void main() {
    vec4 color1 = texture2D(sTexture1, vTextureCoord);
    vec4 color2 = texture2D(sTexture1, vTextureCoord);
    gl_FragColor = color2 * color1;
}
