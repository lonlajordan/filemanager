let $ = jQuery;
let ctx = $("meta[name='ctx']").attr("content");
let options = {
    background: 'rgba(255, 255, 255, 0.75)'
}

function selectItems(checkBox){
    $("#main-table tbody tr input[type=checkbox]").each(function () { this.checked = checkBox.checked; })
}

function reverseSelection(){
    $("#main-table tbody tr input[type=checkbox]").each(function () { this.checked = !this.checked; })
}

function selectItem(checkBox){
    if(!checkBox.checked) $("#js-select-all-items").prop( "checked", false);
}

function deleteItem(id, url){
    if(confirm("Voulez vous vraiment supprimer cet élément ?")){
        fetch(ctx + '/' + url + '/' + id, false);
    }
}

function deleteItems(url){
    let selected = $("#main-table tbody tr input[type=checkbox]:checked");
    let n = selected.length;
    if(n === 0){
        alert('Aucun élément sélectionné');
    }else{
        let params = [];
        selected.each(function () {
            params.push('ids=' + $(this).attr('title'));
        });
        params = params.join("&");
        if(confirm("Voulez-vous vraiment supprimer " + (n === 1 ? 'cet élément ?' : 'ces ' + n + ' éléments ?'))){
            fetch(ctx + '/' + url + '?' + params, false);
        }
    }
}

function submitForm(event) {
    let n = $("#main-table tbody tr input[type=checkbox]:checked").length;
    if(n === 0){
        alert('Sélectionnez au moins un dossier/fichier');
        return false;
    }
    let form = $(event.target);
    let deletion = $(event.submitter).hasClass('delete');
    let copy = $(event.submitter).hasClass('copy');
    let cut = $(event.submitter).hasClass('cut');
    if(deletion){
        if(!confirm("Voulez-vous vraiment supprimer " + (n < 2 ? "cet élément" :  "ces " + n + " éléments") + " ?")){
            return false;
        }
        form.attr('action', ctx + '/delete/files');
    }else if(copy){
        form.attr('action', ctx + '/move/files?action=copy');
    }else if(cut){
        form.attr('action', ctx + '/move/files?action=cut');
    }else{
        event.preventDefault();
        downloadFile(form.serialize(), '/download/files');
    }
}

function deleteFile(event, isDirectory){
    if(!confirm("Voulez-vous vraiment supprimer ce " + (isDirectory ? "dossier" :  "fichier") + " ?")){
        event.preventDefault();
    }
}

function renameFile(event, oldName, id) {
    let newName = prompt("Nouveau nom", oldName);
    if(newName){
        let selector = "#" + id;
        let href = $(selector).attr('href');
        let index = href.lastIndexOf('&name');
        if(index < 0){
            href += "&name=" + newName;
        }else{
            href = href.substring(0, index) + "&name=" + newName;
        }
        $(selector).attr("href", href);
    }else{
        event.preventDefault();
    }
}

function createFolder(event) {
    let folderName = prompt("Nom du dossier", "Nouveau dossier");
    if(folderName){
        let href = $("#create-folder").attr('href');
        let index = href.lastIndexOf('&name');
        if(index < 0){
            href += "&name=" + folderName;
        }else{
            href = href.substring(0, index) + "&name=" + folderName;
        }
        $("#create-folder").attr("href", href);
    }else{
        event.preventDefault();
    }
}

function downloadFile(path, url) {
    $("#wrapper").LoadingOverlay('show', options);
    let xhr = new XMLHttpRequest();
    if(url.includes('files')){
        url = ctx + url + "?" + path;
    }else{
        url = ctx + url + "?path=" + path;
    }
    xhr.open('GET', url, true);
    xhr.responseType = 'arraybuffer';
    xhr.onload = function() {
        if(this.status === 200) {
            let filename = '';
            //get the filename from the header.
            let disposition = xhr.getResponseHeader('Content-Disposition');
            if (disposition && disposition.indexOf('attachment') !== -1) {
                let filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                let matches = filenameRegex.exec(disposition);
                if (matches !== null && matches[1])
                    filename = matches[1].replace(/['"]/g, '');
            }
            let type = xhr.getResponseHeader('Content-Type');
            let blob = new Blob([this.response],  {type: type});
            //workaround for IE
            if(typeof window.navigator.msSaveBlob != 'undefined') {
                window.navigator.msSaveBlob(blob, filename);
            } else {
                let URL = window.URL || window.webkitURL;
                let download_URL = URL.createObjectURL(blob);
                if(filename) {
                    let a_link = document.createElement('a');
                    if(typeof a_link.download == 'undefined') {
                        window.location = download_URL;
                    }else {
                        a_link.href = download_URL;
                        a_link.download = filename;
                        document.body.appendChild(a_link);
                        a_link.click();
                    }
                }else {
                    window.location = download_URL;
                }
                setTimeout(function() {
                    URL.revokeObjectURL(download_URL);
                }, 10000);
            }
        }else {
            alert('fichier introuvable');
        }
        $("#main-table tbody tr input[type=checkbox]").each(function () { this.checked = false; });
        $("#js-select-all-items").prop( "checked", false);
        $('#wrapper').LoadingOverlay('hide', options);
        $('#wrapper').scrollTop(0);
    };
    xhr.setRequestHeader('Content-type', 'application/*');
    xhr.send();
}

function sendBankFiles() {
    document.getElementById('uploadForm').submit();
}

$.ajaxSetup({
    beforeSend: function () {
        $("#wrapper").LoadingOverlay('show', options);
    },
    complete: function () {
        $('#wrapper').LoadingOverlay('hide', options);
        $('#wrapper').scrollTop(0);
    }
})

function fetch(url){
    $.get(url, function (data) {});
}

$(document).ready( function () {
    let table = $('#main-table'),
    length = table.find('th').length,
    targets = [length-1];
    let mainTable = table.DataTable({"paging": false, "info": false, "columnDefs": [{"targets": targets, "orderable": false}]});
    $('#search-addon').on('keyup', function () {
        mainTable.search(this.value).draw();
    });
    $(".lazy-link").each(function () {
        let element = this;
        element.addEventListener("click", function (event) {
            event.preventDefault();
            let url = $(this).attr('href');
            fetch(url);
        })
    })
});