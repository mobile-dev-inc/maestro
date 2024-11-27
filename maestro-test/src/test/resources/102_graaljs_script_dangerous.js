var BigDec = Java.type('java.math.BigDecimal');
var bd = new BigDec("0.1");
console.log(bd.add(bd).toString());
