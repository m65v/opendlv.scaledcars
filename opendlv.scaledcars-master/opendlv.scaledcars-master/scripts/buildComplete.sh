#!/bin/bash
#if you dont have xterm on your linux distro, run "sudo apt install xterm"

xterm -hold -e "cd $HOME/CLionProjects/opendlv.scaledcars/docker && make updateDockerBaseImage && make buildComplete && make createDockerImage && make removeNoneImagesFromDocker" &

