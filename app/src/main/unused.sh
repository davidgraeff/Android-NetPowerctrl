for resourcefile in `find res/values/*.xml`; do
  for stringname in `grep '.*/\1/g'`; do
    count1=`grep -rc "R.string.${stringname}" java | egrep -v ':0$' | wc -l`
    count2=`grep -rc "@string/${stringname}" res/layout | egrep -v ':0$' | wc -l`
    count3=`grep -rc "@string/${stringname}" res/menu | egrep -v ':0$' | wc -l`
    count4=`grep -rc "@string/${stringname}" AndroidManifest.xml | egrep -v '^0$' | wc -l`
    count5=`grep -rc "@string/${stringname}" res/xml | egrep -v ':0$' | wc -l`
    if [ $count1 -eq 0 -a $count2 -eq 0 -a $count3 -eq 0 -a $count4 -eq 0 -a $count5 -eq 0 ]; then
      echo $resourcefile : $stringname
    fi
  done
done

for resourcename in `find res/drawable* -type f -name '*.???'`; do
  resource=`echo $resourcename | xargs basename | sed "s/^\(.*\)\....$/\1/g"`
  count1=`grep -rc "R\.drawable\.${resource}" java | egrep -v ':0$' | wc -l`
  count2=`grep -rc "@drawable/${resource}" res/layout | egrep -v ':0$' | wc -l`
  count3=`grep -rc "@drawable/${resource}" res/drawable*/*.xml | egrep -v ':0$' | wc -l`
  count4=`grep -rc "@drawable/${resource}" res/menu | egrep -v ':0$' | wc -l`
  count5=`grep -rc "@drawable/${resource}" AndroidManifest.xml | egrep -v '^0$' | wc -l`
  if [ $count1 -eq 0 -a $count2 -eq 0 -a $count3 -eq 0 -a $count4 -eq 0 -a $count5 -eq 0 ]; then
      echo $resourcename
  fi
done

for resourcename in `find res/layout/*.xml`; do
  resource=`echo $resourcename | xargs basename | sed "s/^\(.*\)\....$/\1/g"`
  count1=`grep -rc "R\.layout\.${resource}" java | egrep -v ':0$' | wc -l`
  if [ $count1 -eq 0 ]; then
      echo $resourcename
  fi
done