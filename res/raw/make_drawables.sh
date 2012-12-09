#!/bin/sh

convert  -background transparent  outlet.svg -resize 36x36 ../drawable-ldpi/netpowerctrl.png
convert  -background transparent  outlet.svg -resize 48x48 ../drawable-mdpi/netpowerctrl.png
convert  -background transparent  outlet.svg -resize 72x72 ../drawable-hdpi/netpowerctrl.png
convert  -background transparent  outlet.svg -resize 96x96 ../drawable-xhdpi/netpowerctrl.png

convert  -background transparent  widget.svg -resize 36x36 ../drawable-ldpi/widget.png
convert  -background transparent  widget.svg -resize 48x48 ../drawable-mdpi/widget.png
convert  -background transparent  widget.svg -resize 72x72 ../drawable-hdpi/widget.png
convert  -background transparent  widget.svg -resize 96x96 ../drawable-xhdpi/widget.png
