/*
 * Copyright (C) 2015 Jorge Ruesga
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
uniform sampler2D sTexture;
uniform vec4 vColor;

void main() {
    vec4 tex = texture2D (sTexture, vTextureCoord);
    float r = tex.r + (vColor.r - tex.r) * vColor.a;
    float g = tex.g + (vColor.g - tex.g) * vColor.a;
    float b = tex.b + (vColor.b - tex.b) * vColor.a;
    gl_FragColor = vec4(r, g, b, tex.a);
}
