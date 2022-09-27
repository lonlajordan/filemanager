let $ = jQuery;
let ctx = $("meta[name='ctx']").attr("content");
let options = {
    background: 'rgba(255, 255, 255, 0.75)'
}

function changeInstitution(event) {
    let institution = event.target.value;
    if(institution.includes('GIE')){
        $('#privilege-radio-group').addClass('d-none');
    }else{
        $('#privilege-radio-group').removeClass('d-none');
    }
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
    $("#delete-message").text("Voulez-vous vraiment supprimer cet élément ?");
    if(url.includes('user')) $("#delete-message").text("Voulez-vous vraiment supprimer cet utilisateur ?");
    let container = document.getElementById("delete-objects");
    container.innerHTML = '';
    let input = document.createElement("input");
    input.type = "hidden";
    input.name = "ids";
    input.value = id;
    container.appendChild(input);
    $("#modal-delete").modal('show');
}

function deleteFile(path, isDirectory){
    $("#delete-message").text("Voulez-vous vraiment supprimer ce " + (isDirectory ? "dossier" :  "fichier") + " ?");
    let container = document.getElementById("delete-objects");
    container.innerHTML = '';
    let input = document.createElement("input");
    input.type = "hidden";
    input.name = "paths";
    input.value = path;
    container.appendChild(input);
    $("#modal-delete").modal('show');
}

function renameFile(path, oldName) {
    $("#file-path").val(path);
    $("#new-name").val(oldName);
    $("#modal-rename").modal('show');
}

function editUser(id, name, institution, role) {
    $("#user-id").val(id);
    $("#user-name").val(name);
    $("#user-institution").val(institution);
    if(!institution.includes('GIE')){
        let isMonet = role.includes('MONET');
        $('#role-monet').prop('checked', isMonet);
        $('#role-info').prop('checked', !isMonet);
        $('#privilege-radio-group').removeClass('d-none');
    }
    $("#modal-create").modal('show');
}

function invoke(action, object = 'file') {
    let values = $.makeArray($("#main-table tbody input[type=checkbox]:checked")).map(checkbox => $(checkbox).val());
    if(values === undefined || values.length === 0){
        new SnackBar({
            message: 'Aucun élément sélectionné',
            status: 'error',
            dismissible: false,
            position: 'bc',
            fixed: true,
            timeout: 3000,
        });
    }else{
        if(action === 'download'){
            downloadFile(values, '/download/files');
        }else if(action === 'copy' || action === 'cut'){
            let params = values.map(path => 'paths=' + path).join('&');
            window.location = ctx + '/move/files?action=' + action + '&' + params;
        }else if(action === 'delete'){
            let objectName = 'élément';
            switch (object) {
                case 'user':
                    objectName = 'utilisateur';
                    break;
                case 'event':
                    objectName = 'évènement';
                    break;
                default:
                    break;
            }
            $("#delete-message").text("Voulez-vous vraiment supprimer " + (values.length < 2 ? "cet " + objectName :  "ces " + values.length + " " + objectName + "s") + " ?");
            let container = document.getElementById("delete-objects");
            container.innerHTML = '';
            for(let value of values){
                let input = document.createElement("input");
                input.type = "hidden";
                input.value = value;
                input.name = object === 'file' ? 'paths' : 'ids';
                container.appendChild(input);
            }
            $("#modal-delete").modal('show');
        }
    }
}

function downloadFile(paths, url) {
    $("#wrapper").LoadingOverlay('show', options);
    let xhr = new XMLHttpRequest();
    let params = paths.map(path => 'path=' + path).join('&');
    xhr.open('GET', ctx + url + '?' + params, true);
    xhr.responseType = 'arraybuffer';
    xhr.onload = function() {
        if(this.status === 200) {
            let filename = '';
            let disposition = xhr.getResponseHeader('Content-Disposition');
            if (disposition && disposition.indexOf('attachment') !== -1) {
                let filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                let matches = filenameRegex.exec(disposition);
                if (matches !== null && matches[1]) filename = matches[1].replace(/['"]/g, '');
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
            new SnackBar({
                message: 'Téléchargement échoué',
                status: 'error',
                dismissible: false,
                position: 'bc',
                fixed: true,
                timeout: 3000,
            });
        }
        $("#main-table input[type=checkbox]:checked").each(function () { this.checked = false; });
        $('#wrapper').LoadingOverlay('hide', options);
        $('#wrapper').scrollTop(0);
    };
    xhr.setRequestHeader('Content-type', 'application/*');
    xhr.send();
}

function showLogDetails(id, level){
    if(level !== 'ERROR'){
        new SnackBar({
            message: 'Aucun détail concernant cet évènement',
            status: 'error',
            dismissible: false,
            position: 'bc',
            fixed: true,
            timeout: 3000,
        });
        return;
    }
    $("#wrapper").LoadingOverlay('show', options);
    let xhr = new XMLHttpRequest();
    xhr.open('GET', ctx + '/log/details/' + id, true);
    xhr.onload = function() {
        if(this.status === 200 && this.response !== undefined && this.response.length > 0) {
            $('#error-details').html(this.response);
            $("#modal-details").modal('show');
        }else {
            new SnackBar({
                message: 'Aucun détail concernant cet évènement',
                status: 'error',
                dismissible: false,
                position: 'bc',
                fixed: true,
                timeout: 3000,
            });
        }
        $('#wrapper').LoadingOverlay('hide', options);
        $('#wrapper').scrollTop(0);
    };
    xhr.send();
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

function initPagination(){
    let paginator = $('#pagination');
    if(paginator.length !== 0){
        paginator.pagination({
            dataSource: Array.from(Array(parseInt(paginator.attr('title'))).keys()),
            pageSize: 1,
            pageNumber: parseInt(paginator.attr('aria-placeholder')),
            showGoInput: true,
            showGoButton: true,
            triggerPagingOnInit: false,
            callback: function(data, pagination) {
                window.location = ctx + '/' + paginator.attr('aria-label') + '?p=' + pagination.pageNumber;
            }
        })
    }
}

function fetch(url){
    $.get(url, function (data) {});
}

$(document).ready( function () {
    initPagination();
    let table = $('#main-table').DataTable({"paging": false, info: false});
    $('#search-addon').on('keyup', function () {
        table.search(this.value).draw();
    });
});